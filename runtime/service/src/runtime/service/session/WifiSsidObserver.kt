/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service.session

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

sealed interface WifiSsidObservation {
    data class Connected(val ssid: String) : WifiSsidObservation

    /** No physical Wi-Fi transport is connected. This can mean cellular, Ethernet, or offline. */
    data object NoWifi : WifiSsidObservation

    /** Permission loss, disabled location services, or SSID redaction. */
    data object Unavailable : WifiSsidObservation
}

/**
 * Observes the currently connected physical Wi-Fi network without triggering a scan.
 *
 * Android 12+ requires FLAG_INCLUDE_LOCATION_INFO for SSID-bearing transport information. On older releases
 * WifiManager.connectionInfo is the supported compatibility path.
 */
class WifiSsidObserver(
    context: Context,
    private val onChanged: (WifiSsidObservation) -> Unit,
) {
    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val lock = Any()
    private val legacyNetworks = linkedSetOf<Network>()
    private val capabilityObservations = linkedMapOf<Network, WifiSsidObservation>()
    private var started = false
    private var hasObservation = false
    private var lastObservation: WifiSsidObservation = WifiSsidObservation.NoWifi

    private val request =
        NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

    private val callback: ConnectivityManager.NetworkCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationInfoCallback()
        } else {
            legacyCallback()
        }

    fun start() {
        synchronized(lock) {
            if (started) return
            started = true
        }
        val manager =
            connectivity
                ?: run {
                    emit(WifiSsidObservation.Unavailable)
                    return
                }
        runCatching { manager.registerNetworkCallback(request, callback) }
            .onSuccess { emitInitialObservation() }
            .onFailure { emit(WifiSsidObservation.Unavailable) }
    }

    fun stop() {
        val shouldUnregister =
            synchronized(lock) {
                if (!started) false
                else {
                    started = false
                    legacyNetworks.clear()
                    capabilityObservations.clear()
                    true
                }
            }
        if (shouldUnregister) {
            runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
    }

    /** Re-applies the last observation after a rule edit without treating it as a network change. */
    fun refresh() {
        val observation = synchronized(lock) { lastObservation }
        emit(observation, force = true)
    }

    private fun legacyCallback() =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(lock) { legacyNetworks += network }
                emitLegacyObservation()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                synchronized(lock) { legacyNetworks += network }
                emitLegacyObservation()
            }

            override fun onLost(network: Network) {
                synchronized(lock) { legacyNetworks -= network }
                emitLegacyObservation()
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun locationInfoCallback() =
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val observation =
                    normalizeSsid((capabilities.transportInfo as? WifiInfo)?.ssid)?.let(WifiSsidObservation::Connected)
                        ?: WifiSsidObservation.Unavailable
                val selected =
                    synchronized(lock) {
                        capabilityObservations[network] = observation
                        selectCapabilityObservationLocked()
                    }
                emit(selected)
            }

            override fun onLost(network: Network) {
                val selected =
                    synchronized(lock) {
                        capabilityObservations.remove(network)
                        selectCapabilityObservationLocked()
                    }
                emit(selected)
            }
        }

    @Suppress("DEPRECATION")
    private fun emitLegacyObservation() {
        val hasWifi = synchronized(lock) { legacyNetworks.isNotEmpty() }
        val observation =
            normalizeSsid(wifiManager?.connectionInfo?.ssid)?.let(WifiSsidObservation::Connected)
                ?: if (hasWifi) WifiSsidObservation.Unavailable else WifiSsidObservation.NoWifi
        emit(observation)
    }

    private fun emitInitialObservation() {
        val wifiNetwork =
            connectivity?.allNetworks?.firstOrNull { network ->
                connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        if (wifiNetwork == null) {
            emit(WifiSsidObservation.NoWifi)
            return
        }

        val observation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wifiInfo = connectivity.getNetworkCapabilities(wifiNetwork)?.transportInfo as? WifiInfo
                normalizeSsid(wifiInfo?.ssid)?.let(WifiSsidObservation::Connected) ?: WifiSsidObservation.Unavailable
            } else {
                normalizeSsid(wifiManager?.connectionInfo?.ssid)?.let(WifiSsidObservation::Connected)
                    ?: WifiSsidObservation.Unavailable
            }
        emit(observation)
    }

    private fun selectCapabilityObservationLocked(): WifiSsidObservation {
        if (capabilityObservations.isEmpty()) return WifiSsidObservation.NoWifi
        // isPrimary() is a privileged System API. A normal app cannot safely choose between
        // concurrent Wi-Fi connections, so retain the proxy state until the answer is unambiguous.
        return if (capabilityObservations.size == 1) {
            capabilityObservations.values.first()
        } else {
            WifiSsidObservation.Unavailable
        }
    }

    private fun emit(observation: WifiSsidObservation, force: Boolean = false) {
        val shouldEmit =
            synchronized(lock) {
                if (!started) {
                    false
                } else {
                    val changed = !hasObservation || lastObservation != observation
                    lastObservation = observation
                    hasObservation = true
                    force || changed
                }
            }
        if (shouldEmit) onChanged(observation)
    }

    companion object {
        private const val READ_TIMEOUT_MILLIS = 2_500L

        suspend fun readOnce(context: Context): WifiSsidObservation =
            withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { continuation ->
                    lateinit var observer: WifiSsidObserver
                    observer =
                        WifiSsidObserver(context) { observation ->
                            if (continuation.isActive) {
                                observer.stop()
                                continuation.resume(observation)
                            }
                        }
                    continuation.invokeOnCancellation { observer.stop() }
                    observer.start()
                }
            } ?: WifiSsidObservation.NoWifi

        /** Removes Android's legacy display quotes but preserves every other SSID byte/character. */
        fun normalizeSsid(raw: String?): String? =
            raw?.takeUnless { it == WifiManager.UNKNOWN_SSID }
                ?.let { value ->
                    if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
                        value.substring(1, value.length - 1)
                    } else {
                        value
                    }
                }
                ?.takeIf { it.isNotEmpty() }
    }
}
