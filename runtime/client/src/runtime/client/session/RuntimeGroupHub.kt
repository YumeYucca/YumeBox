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

package com.github.yumeyucca.yumebox.runtime.client.session

import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.resolveTestableProxyNames
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/** Proxy-group cache + refresh/select/health operations owned by the runtime session surface. */
internal class RuntimeGroupHub(
    private val scope: CoroutineScope,
    private val session: RuntimeSession,
    private val coreOps: RuntimeCoreOps,
    private val isRemoteControllerActive: () -> Boolean,
) {
    private companion object {
        const val PROXY_SELECT_FULL_REFRESH_DELAY_MS = 400L
    }

    private val groupStore =
        ProxyGroupStore(
            isRuntimeRunning = { session.snapshotValue().phase == RuntimePhase.Running },
            onGroupsReady = { ready -> session.updateGroupsReady(ready) },
        )
    private val refreshMutex = Mutex()
    private val delayTestMutex = Mutex()
    private val delayedRefreshLock = Any()
    private val clearGeneration = AtomicLong()
    private var scheduledGroupsRefresh: Job? = null

    private data class GroupSessionEpoch(
        val runtimeGeneration: Long,
        val clearGeneration: Long,
    )

    private data class DelayTestContext(
        val epoch: GroupSessionEpoch,
        val groups: List<ProxyGroupInfo>,
    ) {
        val proxiesByGroup = groups.associate { group -> group.name to group.proxies }
    }

    val groups
        get() = groupStore.groups

    val resolvedPrimaryNode
        get() = groupStore.resolvedPrimaryNode

    fun clear(resetGroups: Boolean) {
        clearGeneration.incrementAndGet()
        synchronized(delayedRefreshLock) {
            scheduledGroupsRefresh?.cancel()
            scheduledGroupsRefresh = null
        }
        groupStore.clear(resetGroups)
    }

    fun isGroupsEmpty(): Boolean = groupStore.groups.value.isEmpty()

    suspend fun selectProxy(group: String, proxyName: String): Boolean {
        Timber.d("Select proxy: group=$group proxy=$proxyName")
        val ok = coreOps.patchSelector(group, proxyName)
        if (ok) {
            refreshProxyGroup(group)
            scheduleGroupsRefresh(PROXY_SELECT_FULL_REFRESH_DELAY_MS)
        }
        return ok
    }

    suspend fun healthCheck(group: String, singleNodeTest: Boolean) {
        Timber.d("Health check request: group=%s", group)
        val context =
            delayTestMutex.withLock {
                val testContext = currentDelayTestContext() ?: return@withLock null
                testContext.takeIf { healthCheckGroup(it, group, singleNodeTest) }
            }
        if (context != null) {
            Timber.d("Health check completed: group=%s", group)
            scheduleGroupsRefresh(
                delayMillis = PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                epoch = context.epoch,
            )
        }
    }

    suspend fun healthCheckAll(singleNodeTest: Boolean) {
        Timber.d("Health check all request")
        val context =
            delayTestMutex.withLock {
                val testContext = currentDelayTestContext() ?: return@withLock null
                val testedProxyNames = linkedSetOf<String>()
                for (group in testContext.groups) {
                    if (!healthCheckGroup(testContext, group.name, singleNodeTest, testedProxyNames)) {
                        return@withLock null
                    }
                }
                testContext
            }
        if (context != null) {
            scheduleGroupsRefresh(
                delayMillis = PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                epoch = context.epoch,
            )
        }
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        Timber.d("Health check proxy request: group=%s proxy=%s", group, proxyName)
        val epoch = currentEpoch()
        val delay =
            delayTestMutex.withLock {
                val result = coreOps.healthCheckProxy(group, proxyName)
                publishDirectDelays(epoch, mapOf(proxyName to result))
                result
            }
        Timber.d("Health check proxy done: group=%s proxy=%s delay=%s", group, proxyName, delay)
        if (isCurrentEpoch(epoch)) {
            scheduleGroupsRefresh(
                delayMillis = PollingTimerSpecs.ProxyHealthcheckRefresh.intervalMillis,
                epoch = epoch,
            )
        }
        return delay
    }

    suspend fun refreshProxyGroups() {
        val epoch = currentEpoch()
        refreshMutex.withLock {
            if (!isCurrentEpoch(epoch)) return
            val snapshot = session.snapshotValue()
            var missingLocalRuntime = false
            val groups =
                withContext(Dispatchers.IO) {
                    runCatching {
                        if (!snapshot.running) {
                            return@runCatching queryPreviewProxyGroups()
                        }
                        coreOps
                            .queryAllProxyGroups(excludeNotSelectable = false)
                            .map(groupStore::toInfo)
                        }
                        .getOrElse { error ->
                            if (error is CancellationException) throw error
                            Timber.e(error, "Failed to refresh proxy groups")
                            missingLocalRuntime = session.isMissingLocalRuntime(snapshot)
                            null
                        }
                }

            if (!isCurrentEpoch(epoch)) return
            if (groups != null) {
                groupStore.publish(groupStore.mergeReportedDelays(groups))
            } else if (missingLocalRuntime) {
                session.handleMissingLocalRuntime(snapshot, "runtime backend unavailable")
                runCatching { queryPreviewProxyGroups() }
                    .onSuccess { preview ->
                        if (isCurrentEpoch(epoch)) {
                            groupStore.publish(preview)
                        }
                    }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(
                            error,
                            "Fallback preview refresh skipped after stale runtime reset",
                        )
                    }
            }
        }
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        val epoch = currentEpoch()
        if (!session.snapshotValue().running) {
            if (groupStore.groups.value.isEmpty()) {
                refreshProxyGroups()
            }
            return
        }
        refreshMutex.withLock {
            if (!isCurrentEpoch(epoch)) return
            val updatedGroup =
                withContext(Dispatchers.IO) {
                    runCatching { groupStore.toInfo(coreOps.queryProxyGroup(name, sort)) }
                        .getOrElse { error ->
                            if (error is CancellationException) throw error
                            Timber.e(error, "Failed to refresh proxy group: %s", name)
                            null
                        }
                } ?: return
            if (!isCurrentEpoch(epoch)) return
            val mergedGroup = groupStore.mergeReportedDelays(listOf(updatedGroup)).first()
            groupStore.publish(groupStore.upsert(mergedGroup))
        }
    }

    suspend fun refreshSafely() {
        val snapshot = session.snapshotValue()
        if (
            snapshot.phase != RuntimePhase.Running &&
                snapshot.owner != RuntimeOwner.RemoteController
        ) {
            return
        }
        runCatching { refreshProxyGroups() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Runtime proxy group sync skipped")
            }
    }

    private fun scheduleGroupsRefresh(
        delayMillis: Long = 0L,
        epoch: GroupSessionEpoch = currentEpoch(),
    ) {
        lateinit var job: Job
        synchronized(delayedRefreshLock) {
            scheduledGroupsRefresh?.cancel()
            job =
                scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        awaitDelay(delayMillis, "runtime_proxy_groups_refresh")
                        if (isCurrentEpoch(epoch)) {
                            refreshSafely()
                        }
                    } finally {
                        synchronized(delayedRefreshLock) {
                            if (scheduledGroupsRefresh === job) {
                                scheduledGroupsRefresh = null
                            }
                        }
                    }
                }
            scheduledGroupsRefresh = job
            job.start()
        }
    }

    private suspend fun healthCheckGroup(
        context: DelayTestContext,
        groupName: String,
        singleNodeTest: Boolean,
        testedProxyNames: MutableSet<String> = linkedSetOf(),
    ): Boolean {
        if (!isCurrentEpoch(context.epoch)) return false
        if (!singleNodeTest) {
            val delays = coreOps.healthCheck(groupName)
            return publishDirectDelays(context.epoch, delays)
        }

        val proxyNames = resolveTestableProxyNames(groupName, context.proxiesByGroup)
        if (proxyNames.isEmpty()) {
            val delays = coreOps.healthCheck(groupName)
            return publishDirectDelays(context.epoch, delays)
        }
        for (proxyName in proxyNames) {
            if (!testedProxyNames.add(proxyName)) continue
            if (!isCurrentEpoch(context.epoch)) return false
            val delay = coreOps.healthCheckProxy(groupName, proxyName)
            if (!publishDirectDelays(context.epoch, mapOf(proxyName to delay))) {
                return false
            }
        }
        return isCurrentEpoch(context.epoch)
    }

    private suspend fun currentDelayTestContext(): DelayTestContext? {
        val epoch = currentEpoch()
        val freshGroups =
            withContext(Dispatchers.IO) {
                runCatching {
                    coreOps
                        .queryAllProxyGroups(excludeNotSelectable = false)
                        .map(groupStore::toInfo)
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(error, "Delay-test group snapshot unavailable")
                    }
                    .getOrNull()
            } ?: return null
        if (!isCurrentEpoch(epoch)) return null

        refreshMutex.withLock {
            if (!isCurrentEpoch(epoch)) return null
            groupStore.publish(groupStore.mergeReportedDelays(freshGroups))
        }
        return DelayTestContext(epoch, freshGroups)
    }

    private suspend fun publishDirectDelays(
        epoch: GroupSessionEpoch,
        delays: Map<String, Int>,
    ): Boolean {
        if (!isCurrentEpoch(epoch)) return false
        if (delays.none { (_, delay) -> delay != 0 }) {
            return true
        }
        return refreshMutex.withLock {
            if (!isCurrentEpoch(epoch)) {
                false
            } else {
                groupStore.publish(groupStore.recordDirectDelaysEverywhere(delays))
                true
            }
        }
    }

    private fun currentEpoch(): GroupSessionEpoch =
        GroupSessionEpoch(
            runtimeGeneration = session.snapshotValue().generation,
            clearGeneration = clearGeneration.get(),
        )

    private fun isCurrentEpoch(epoch: GroupSessionEpoch): Boolean =
        session.snapshotValue().generation == epoch.runtimeGeneration &&
            clearGeneration.get() == epoch.clearGeneration

    private suspend fun awaitDelay(delayMillis: Long, name: String) {
        if (delayMillis <= 0L) return
        PollingTimers.awaitTick(
            PollingTimerSpecs.dynamic(
                name = name,
                intervalMillis = delayMillis,
                initialDelayMillis = delayMillis,
            )
        )
    }

    private suspend fun queryPreviewProxyGroups(): List<ProxyGroupInfo> {
        if (isRemoteControllerActive()) {
            return coreOps.queryAllProxyGroups(excludeNotSelectable = false).map(groupStore::toInfo)
        }
        session.connectBackend()
        val activeProfile =
            RuntimeAccess.profile().queryActive().also {
                session.setCurrentProfile(it)
                session.updateProfileReady(it)
            }
        if (activeProfile == null) {
            return emptyList()
        }
        return coreOps.queryAllProxyGroups(excludeNotSelectable = false).map(groupStore::toInfo)
    }
}
