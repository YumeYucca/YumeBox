/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.data.controller

import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.model.AppTrafficDeltaRecord
import com.github.yumeyucca.yumebox.data.model.ConnectionTrafficBaseline
import com.github.yumeyucca.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumeyucca.yumebox.data.store.TrafficStatisticsStore
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class AppTrafficStatisticsCollector(
    private val querySource: TrafficQuerySource,
    private val trafficStatisticsStore: TrafficStatisticsStore,
    private val appIdentityResolver: AppIdentityResolver,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var collectionJob: Job? = null
    private var monitoringJob: Job? = null
    private val connectionBaselines = linkedMapOf<String, ConnectionTrafficBaseline>()
    private var lastTotalUpload = NO_BASELINE
    private var lastTotalDownload = NO_BASELINE
    private var lastProfileId: String? = null
    private var lastCollectionAtNanos = 0L

    init {
        startCollection()
    }

    private fun startCollection() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            querySource.isRunning.collect { isRunning ->
                val previousMonitoring = monitoringJob
                previousMonitoring?.cancelAndJoin()
                monitoringJob = null
                if (isRunning) {
                    monitoringJob = startTrafficMonitoring()
                } else {
                    if (previousMonitoring != null) {
                        collectTrafficDataSafely()
                    }
                    resetBaselines()
                }
            }
        }
    }

    private fun startTrafficMonitoring(): Job = scope.launch {
        lastTotalUpload = trafficStatisticsStore.getLastTrafficUpload()
        lastTotalDownload = trafficStatisticsStore.getLastTrafficDownload()
        lastProfileId = trafficStatisticsStore.getLastProfileId()
        lastCollectionAtNanos = 0L
        connectionBaselines.clear()
        collectTrafficDataSafely()
        PollingTimers.ticks(PollingTimerSpecs.TrafficStatsCollection).collect { collectTrafficDataSafely() }
    }

    private suspend fun collectTrafficDataSafely() {
        runCatching { collectTrafficData() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.tag(TAG).e(error, "App traffic collection failed")
            }
    }

    private suspend fun collectTrafficData() {
        val snapshot = querySource.queryConnections()
        val totalTraffic =
            TrafficData(
                upload = snapshot.uploadTotal,
                download = snapshot.downloadTotal,
            )
        val nowNanos = System.nanoTime()
        val elapsedMs =
            if (lastCollectionAtNanos == 0L) {
                0L
            } else {
                ((nowNanos - lastCollectionAtNanos) / NANOS_PER_MILLI).coerceAtLeast(1L)
            }
        lastCollectionAtNanos = nowNanos
        val timestamp = System.currentTimeMillis()
        val currentProfileId = querySource.currentProfileId() ?: bestEffortActiveProfileId()

        if (lastTotalUpload < 0L || lastTotalDownload < 0L || currentProfileId != lastProfileId) {
            rebaseline(totalTraffic, currentProfileId, snapshot.connections)
            return
        }

        if (connectionBaselines.isEmpty()) {
            // Never record the persisted gap as one delta after a process restart.
            rebaseline(totalTraffic, currentProfileId, snapshot.connections)
            return
        }

        if (totalTraffic.upload < lastTotalUpload || totalTraffic.download < lastTotalDownload) {
            rebaseline(totalTraffic, currentProfileId, snapshot.connections)
            return
        }

        val trafficDeltas = linkedMapOf<String, AppTrafficDeltaRecord>()
        val attributedTotals =
            collectConnectionDeltas(
                connections = snapshot.connections,
                trafficDeltas = trafficDeltas,
            )
        val totalUploadDelta = totalTraffic.upload - lastTotalUpload
        val totalDownloadDelta = totalTraffic.download - lastTotalDownload
        if (isImplausibleDelta(totalUploadDelta, elapsedMs) || isImplausibleDelta(totalDownloadDelta, elapsedMs)) {
            rebaseline(totalTraffic, currentProfileId, snapshot.connections)
            return
        }
        val unattributedUpload = (totalUploadDelta - attributedTotals.upload).coerceAtLeast(0L)
        val unattributedDownload = (totalDownloadDelta - attributedTotals.download).coerceAtLeast(0L)
        if (unattributedUpload > 0L || unattributedDownload > 0L) {
            trafficDeltas[TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY] =
                TrafficStatisticsBuckets.buildUnattributedRecord(
                    uploadDelta = unattributedUpload,
                    downloadDelta = unattributedDownload,
                )
        }

        if (trafficDeltas.isNotEmpty()) {
            trafficStatisticsStore.recordAppTrafficBatch(
                timestamp = timestamp,
                records = trafficDeltas.values.toList(),
            )
        }
        initializeTotals(
            totalTraffic = totalTraffic,
            profileId = currentProfileId,
        )
    }

    private fun collectConnectionDeltas(
        connections: List<ConnectionInfo>,
        trafficDeltas: MutableMap<String, AppTrafficDeltaRecord>,
    ): TrafficData {
        val activeIds = hashSetOf<String>()
        var attributedUpload = 0L
        var attributedDownload = 0L

        connections.forEach { connection ->
            activeIds += connection.id
            val (uploadDelta, downloadDelta) =
                collectConnectionDelta(connection = connection, trafficDeltas = trafficDeltas)
            attributedUpload += uploadDelta
            attributedDownload += downloadDelta
        }

        connectionBaselines.keys.retainAll(activeIds)
        return TrafficData(attributedUpload, attributedDownload)
    }

    private fun collectConnectionDelta(
        connection: ConnectionInfo,
        trafficDeltas: MutableMap<String, AppTrafficDeltaRecord>,
    ): TrafficData {
        val baseline = connectionBaselines[connection.id]
        val identity = resolveIdentity(connection, baseline)

        val updatedBaseline = baselineFor(connection, identity)

        if (baseline == null) {
            connectionBaselines[connection.id] = updatedBaseline
            return TrafficData.zero
        }

        if (connection.upload < baseline.upload || connection.download < baseline.download) {
            connectionBaselines[connection.id] = updatedBaseline
            return TrafficData.zero
        }

        val uploadDelta = connection.upload - baseline.upload
        val downloadDelta = connection.download - baseline.download

        if (uploadDelta > 0L || downloadDelta > 0L) {
            val routeKey = resolveRouteKey(connection)
            val routeLabel = resolveRouteLabel(connection, routeKey)
            val trafficKey = buildTrafficBucketKey(identity.appKey, routeKey)
            val current = trafficDeltas[trafficKey]
            trafficDeltas[trafficKey] =
                AppTrafficDeltaRecord(
                    appKey = identity.appKey,
                    packageName = identity.packageName ?: current?.packageName,
                    appName = identity.appName.ifBlank { current?.appName.orEmpty() },
                    uploadDelta = (current?.uploadDelta ?: 0L) + uploadDelta,
                    downloadDelta = (current?.downloadDelta ?: 0L) + downloadDelta,
                    routeKey = routeKey,
                    routeLabel = routeLabel,
                )
        }

        connectionBaselines[connection.id] = updatedBaseline
        return TrafficData(uploadDelta, downloadDelta)
    }

    private fun resolveIdentity(
        connection: ConnectionInfo,
        baseline: ConnectionTrafficBaseline?,
    ): AppIdentity {
        baseline?.let {
            return AppIdentity(
                appKey = it.appKey,
                packageName = it.packageName,
                appName = it.appName,
            )
        }
        return appIdentityResolver.resolve(connection.metadata)
    }

    /** Starts a fresh measurement window: totals and per-connection baselines move together. */
    private fun rebaseline(
        totalTraffic: TrafficData,
        profileId: String?,
        connections: List<ConnectionInfo>,
    ) {
        initializeTotals(totalTraffic = totalTraffic, profileId = profileId)
        bootstrapConnectionBaselines(connections)
    }

    /**
     * Active profile id from the runtime, or null when the runtime has not published one yet. Best effort only, but
     * cancellation must still reach the collector's owner.
     */
    private suspend fun bestEffortActiveProfileId(): String? =
        try {
            querySource.queryActiveProfileId()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.tag(TAG).d(error, "Active profile id unavailable")
            null
        }

    private fun bootstrapConnectionBaselines(connections: List<ConnectionInfo>) {
        connectionBaselines.clear()
        connections.forEach { connection ->
            connectionBaselines[connection.id] =
                baselineFor(connection, appIdentityResolver.resolve(connection.metadata))
        }
    }

    private fun baselineFor(connection: ConnectionInfo, identity: AppIdentity) =
        ConnectionTrafficBaseline(
            upload = connection.upload,
            download = connection.download,
            appKey = identity.appKey,
            packageName = identity.packageName,
            appName = identity.appName,
        )

    private fun initializeTotals(
        totalTraffic: TrafficData,
        profileId: String?,
    ) {
        lastTotalUpload = totalTraffic.upload
        lastTotalDownload = totalTraffic.download
        lastProfileId = profileId
        trafficStatisticsStore.setLastTraffic(
            upload = totalTraffic.upload,
            download = totalTraffic.download,
            profileId = profileId,
        )
    }

    private fun resetBaselines() {
        connectionBaselines.clear()
        lastTotalUpload = NO_BASELINE
        lastTotalDownload = NO_BASELINE
        lastProfileId = null
        lastCollectionAtNanos = 0L
    }

    /**
     * A corrupt/transient total must not be stored as one enormous delta. The cap is deliberately far above any real
     * device throughput; it only catches impossible jumps.
     */
    private fun isImplausibleDelta(delta: Long, elapsedMs: Long): Boolean {
        if (elapsedMs <= 0L) return false
        val maxDelta = elapsedMs.coerceAtMost(Long.MAX_VALUE / MAX_BYTES_PER_MILLI) * MAX_BYTES_PER_MILLI
        return delta > maxDelta
    }

    private fun buildTrafficBucketKey(appKey: String, routeKey: String?): String =
        if (routeKey.isNullOrBlank()) appKey else "$appKey::$routeKey"

    /** Chain identity of the connection; the single place that decides which chain wins. */
    private fun preferredChainLabel(connection: ConnectionInfo): String? =
        connection.chains.lastOrNull()?.takeIf(String::isNotBlank)
            ?: connection.providerChains.lastOrNull()?.takeIf(String::isNotBlank)

    private fun resolveRouteKey(connection: ConnectionInfo): String =
        preferredChainLabel(connection)
            ?: connection.rulePayload.takeIf(String::isNotBlank)
            ?: connection.rule.takeIf(String::isNotBlank)
            ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_KEY

    private fun resolveRouteLabel(connection: ConnectionInfo, routeKey: String?): String =
        preferredChainLabel(connection) ?: routeKey ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_NAME

    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
        collectionJob?.cancel()
        collectionJob = null
        connectionBaselines.clear()
    }

    companion object {
        private const val TAG = "AppTrafficStatsCollector"
        private const val NO_BASELINE = -1L
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val MAX_BYTES_PER_MILLI = 64L * 1024L * 1024L
    }
}
