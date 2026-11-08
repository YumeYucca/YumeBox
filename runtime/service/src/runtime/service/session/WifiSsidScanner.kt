/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

sealed interface WifiSsidScanResult {
    data class Success(val networks: List<WifiSsidNetwork>) : WifiSsidScanResult

    /** Wi-Fi is disabled, scan access is unavailable, or Android rejected the request. */
    data object Unavailable : WifiSsidScanResult
}

/** One visible SSID together with the strongest scanned radio's public capability metadata. */
data class WifiSsidNetwork(
    val ssid: String,
    val band: String?,
    val wifiGeneration: String?,
) {
    val label: String?
        get() {
            val parts = listOfNotNull(band, wifiGeneration)
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }
}

/** Performs one explicit, user-initiated Wi-Fi scan. It never runs in the background. */
object WifiSsidScanner {
    private const val SCAN_TIMEOUT_MILLIS = 8_000L

    @Suppress("DEPRECATION")
    suspend fun scanOnce(context: Context): WifiSsidScanResult {
        val appContext = context.applicationContext
        val wifiManager = appContext.getSystemService(WifiManager::class.java) ?: return WifiSsidScanResult.Unavailable

        return withTimeoutOrNull(SCAN_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                var registered = false
                lateinit var receiver: BroadcastReceiver

                fun snapshot(): WifiSsidScanResult = runCatching {
                    WifiSsidScanResult.Success(scanNetworks(wifiManager))
                }
                    .getOrElse { WifiSsidScanResult.Unavailable }

                fun finish(result: WifiSsidScanResult) {
                    if (registered) {
                        runCatching { appContext.unregisterReceiver(receiver) }
                        registered = false
                    }
                    if (continuation.isActive) continuation.resume(result)
                }

                receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) finish(snapshot())
                        }
                    }

                runCatching {
                    ContextCompat.registerReceiver(
                        appContext,
                        receiver,
                        IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    registered = true
                    if (!wifiManager.startScan()) finish(snapshot())
                }
                    .onFailure { finish(WifiSsidScanResult.Unavailable) }

                continuation.invokeOnCancellation {
                    if (registered) runCatching { appContext.unregisterReceiver(receiver) }
                }
            }
        }
            ?: runCatching {
                WifiSsidScanResult.Success(scanNetworks(wifiManager))
            }
                .getOrElse { WifiSsidScanResult.Unavailable }
    }

    @Suppress("DEPRECATION")
    private fun scanNetworks(wifiManager: WifiManager): List<WifiSsidNetwork> =
        wifiManager.scanResults
            .mapNotNull { result -> WifiSsidObserver.normalizeSsid(result.SSID)?.let { ssid -> ssid to result } }
            .groupBy({ it.first }, { it.second })
            .map { (ssid, results) ->
                results
                    .maxBy { it.level }
                    .let { result ->
                        WifiSsidNetwork(
                            ssid = ssid,
                            band = frequencyBand(result.frequency),
                            wifiGeneration = wifiGeneration(result),
                        )
                    }
            }
            .sortedBy(WifiSsidNetwork::ssid)

    private fun frequencyBand(frequencyMhz: Int): String? =
        when (frequencyMhz) {
            in 2_400..2_500 -> "2.4G"
            in 4_900..5_900 -> "5G"
            in 5_925..7_125 -> "6G"
            in 57_000..71_000 -> "60G"
            else -> null
        }

    private fun wifiGeneration(result: ScanResult): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wifiGenerationOnR(result) else null

    @RequiresApi(Build.VERSION_CODES.R)
    private fun wifiGenerationOnR(result: ScanResult): String? =
        when (result.wifiStandard) {
            ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4"
            ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5"
            ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6"
            ScanResult.WIFI_STANDARD_11AD -> "WiGig"
            ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7"
            else -> null
        }
}
