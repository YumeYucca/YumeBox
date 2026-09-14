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

@file:Suppress("ConvertLongToDuration")

package com.github.yumeyucca.yumebox.runtime.service.controller

import com.github.yumeyucca.yumebox.core.bridge.UnixSocketFactory
import com.github.yumeyucca.yumebox.core.model.*
import com.github.yumeyucca.yumebox.core.util.encodeTrafficValue
import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.api.CoreAsyncQueries
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant

/** [CoreApi] over mihomo REST: local unix socket and/or remote TCP backend. */
class CoreController(
    private val local: Local? = null,
    private val backendProvider: () -> RemoteBackend? = { null },
) : CoreApi, CoreAsyncQueries {

    /** Local controller endpoint: fixed socket path, secret read per request. */
    class Local(val socketPath: String, val secret: () -> String)

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var trafficSampleCache: TimedTrafficSample? = null

    /** Off-critical-path refreshes (see [providerSnapshot]); never joined by a request path. */
    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var providerSnapshotCache: TimedProviderSnapshot? = null
    @Volatile private var providerSnapshotJob: Deferred<ProviderSnapshot>? = null

    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = REQUEST_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            local?.let { target ->
                engine { config { socketFactory(UnixSocketFactory(target.socketPath)) } }
            }
        }
    }

    private val logStream by lazy {
        CoreControllerLogStream(
            client = client,
            json = json,
            logUrl = { buildUrl("logs", query = LOG_QUERY) },
            applyAuth = { applyAuth() },
        )
    }

    /** Active target secret (local token or remote backend). */
    private fun activeSecret(): String =
        local?.secret?.invoke() ?: (backendProvider()?.secret.orEmpty())

    private fun ensureEndpointReady() {
        if (local != null) return
        if (backendProvider() == null) {
            error("No active remote controller backend")
        }
    }

    private fun requireBackend(): RemoteBackend =
        backendProvider() ?: error("No active remote controller backend")

    /**
     * Reachability probe for a remote backend (`GET /configs`). Always hits [backendProvider]; do
     * not call this on a local unix-socket controller.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun probe(): Boolean {
        if (local != null) return false
        return try {
            queryTunnelStateAsync()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Remote controller probe failed")
            false
        }
    }

    /** Absolute URL under the active endpoint base. */
    private fun buildUrl(
        vararg pathSegments: String,
        query: Map<String, String> = emptyMap(),
    ): String {
        val base = if (local != null) LOCAL_BASE_URL else requireBackend().normalizedBaseUrl
        return URLBuilder(base)
            .apply {
                appendPathSegments(*pathSegments)
                query.forEach { (key, value) -> parameters.append(key, value) }
            }
            .buildString()
    }

    private suspend fun request(
        method: HttpMethod,
        vararg pathSegments: String,
        query: Map<String, String> = emptyMap(),
        body: Any? = null,
    ): HttpResponse {
        ensureEndpointReady()
        val url = buildUrl(*pathSegments, query = query)
        return when (method) {
            HttpMethod.Get -> client.get(url) { applyAuth() }

            HttpMethod.Delete -> client.delete(url) { applyAuth() }

            HttpMethod.Put ->
                client.put(url) {
                    applyAuth()
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }

            HttpMethod.Patch ->
                client.patch(url) {
                    applyAuth()
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }

            else -> error("Unsupported HTTP method: $method")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth() {
        val secret = activeSecret()
        if (secret.isNotBlank()) {
            header(HttpHeaders.Authorization, "Bearer $secret")
        }
    }

    /** Encode free-form JSON body without relying on erased Map serializers. */
    private inline fun <reified T> jsonBody(value: T): TextContent =
        TextContent(json.encodeToString(value), ContentType.Application.Json)

    override suspend fun queryTunnelStateAsync(): TunnelState {
        val raw = request(HttpMethod.Get, "configs").bodyAsText()
        val configs = json.decodeFromString<RawConfigs>(raw)
        return TunnelState(configs.mode)
    }

    override fun queryTunnelState(): TunnelState =
        runBlocking(Dispatchers.IO) { queryTunnelStateAsync() }

    override suspend fun queryTrafficNowAsync(): Long {
        val sample = readTrafficSample() ?: return 0L
        return (encodeTrafficValue(sample.up) shl 32) or encodeTrafficValue(sample.down)
    }

    override fun queryTrafficNow(): Long = runBlocking(Dispatchers.IO) { queryTrafficNowAsync() }

    override suspend fun queryTrafficTotalAsync(): Long {
        val sample = readTrafficSample() ?: return 0L
        return (encodeTrafficValue(sample.upTotal) shl 32) or encodeTrafficValue(sample.downTotal)
    }

    override fun queryTrafficTotal(): Long =
        runBlocking(Dispatchers.IO) { queryTrafficTotalAsync() }

    /** Reuse one stream sample for the adjacent now/total reads performed by the UI. */
    private suspend fun readTrafficSample(): RawTraffic? {
        val now = System.nanoTime()
        trafficSampleCache?.takeIf { now - it.capturedAtNanos <= TRAFFIC_SAMPLE_CACHE_NS }?.let {
            return it.sample
        }
        return runCatching {
                client
                    .prepareGet(buildUrl("traffic")) { applyAuth() }
                    .execute { response ->
                        val line = response.bodyAsChannel().readLine()
                        line?.let { json.decodeFromString<RawTraffic>(it) }
                    }
            }
            .getOrNull()
            ?.also { sample ->
                trafficSampleCache = TimedTrafficSample(sample, System.nanoTime())
            }
    }

    override suspend fun queryConnectionsAsync(): ConnectionSnapshot = fetchConnections()

    override fun queryConnections(): ConnectionSnapshot =
        runBlocking(Dispatchers.IO) { queryConnectionsAsync() }

    private suspend fun fetchConnections(): ConnectionSnapshot =
        client
            .prepareGet(buildUrl("connections", query = CONNECTIONS_QUERY)) {
                applyAuth()
            }
            .execute { response ->
                val line =
                    response.bodyAsChannel().readLine()
                        ?: error("connections stream ended before the first snapshot")
                json.decodeFromString<ConnectionSnapshot>(line)
            }

    override suspend fun queryAllProxyGroupsAsync(excludeNotSelectable: Boolean): List<ProxyGroup> =
        withGroupQueryTimeout {
            val nodes = fetchProxies()
            val groups = orderGroups(fetchGroups(), nodes)
            groups
                .filter { !excludeNotSelectable || it.type in Proxy.Type.manuallySelectable }
                .map { buildGroup(it, nodes, ProxySort.Default) }
        }

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        runBlocking(Dispatchers.IO) { queryAllProxyGroupsAsync(excludeNotSelectable) }

    override suspend fun queryProxyGroupNamesAsync(excludeNotSelectable: Boolean): List<String> =
        withGroupQueryTimeout {
            val nodes = fetchProxies()
            orderGroups(fetchGroups(), nodes)
                .filter { !excludeNotSelectable || it.type in Proxy.Type.manuallySelectable }
                .map { it.name }
        }

    private suspend fun <T> withGroupQueryTimeout(block: suspend () -> T): T =
        if (local != null) {
            withTimeout(LOCAL_GROUP_QUERY_TIMEOUT_MS) { block() }
        } else {
            block()
        }

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> =
        runBlocking(Dispatchers.IO) { queryProxyGroupNamesAsync(excludeNotSelectable) }

    /** Prefer GLOBAL.all order, then name. */
    private fun orderGroups(groups: List<RawProxy>, nodes: Map<String, RawProxy>): List<RawProxy> {
        val canonical = nodes["GLOBAL"]?.all ?: emptyList()
        val indexOf = canonical.withIndex().associate { (i, name) -> name to i }
        return groups.sortedWith(compareBy({ indexOf[it.name] ?: Int.MAX_VALUE }, { it.name }))
    }

    override suspend fun queryProxyGroupAsync(name: String, proxySort: ProxySort): ProxyGroup {
        val nodes = fetchProxies()
        val group =
            nodes[name]
                ?: return ProxyGroup(
                    name = name,
                    type = Proxy.Type.Unknown,
                    proxies = emptyList(),
                    now = "",
                )
        return buildGroup(group, nodes, proxySort)
    }

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        runBlocking(Dispatchers.IO) { queryProxyGroupAsync(name, proxySort) }

    /**
     * Every node the UI can render, keyed by name.
     *
     * `/proxies` (`tunnel.Proxies()` in the core) is the canonical, immediately available snapshot,
     * but it holds only config-declared outbounds and groups: members a group pulls in through
     * `proxy-providers` are absent, so [buildGroup] used to fall back to a placeholder and render
     * every provider-backed node as type Unknown with no latency. Providers therefore form the base
     * layer and `/proxies` is laid on top, so a config-declared proxy of the same name still wins —
     * the same precedence the core itself resolves.
     *
     * The two fetches run concurrently and the provider half is bounded (see [providerSnapshot]),
     * so this never reintroduces the startup regression that made group queries wait on provider
     * initialization.
     */
    private suspend fun fetchProxies(): Map<String, RawProxy> = coroutineScope {
        val direct = async {
            val raw = request(HttpMethod.Get, "proxies").bodyAsText()
            json.decodeFromString<RawProxiesResponse>(raw).proxies
        }
        providerSnapshot().nodes + direct.await()
    }

    /**
     * Best-effort snapshot of the nodes served by proxy providers, keyed by node name.
     *
     * Deliberately kept off the critical path: the fetch runs on [providerScope] and callers wait
     * at most [PROVIDER_SNAPSHOT_WAIT_MS] for it, so a provider that is slow or still initializing
     * degrades to "not enriched this round" instead of stalling a group query — which local mode
     * bounds at [LOCAL_GROUP_QUERY_TIMEOUT_MS]. The in-flight fetch keeps running and fills the
     * cache for the next call.
     */
    private suspend fun providerSnapshot(): ProviderSnapshot {
        providerSnapshotCache
            ?.takeIf { System.nanoTime() - it.capturedAtNanos <= PROVIDER_SNAPSHOT_CACHE_NS }
            ?.let {
                return it.snapshot
            }
        val refresh = refreshProviderSnapshot()
        return withTimeoutOrNull(PROVIDER_SNAPSHOT_WAIT_MS) { refresh.await() }
            ?: providerSnapshotCache?.snapshot
            ?: ProviderSnapshot.Empty
    }

    @Synchronized
    private fun refreshProviderSnapshot(): Deferred<ProviderSnapshot> {
        providerSnapshotJob?.takeIf { it.isActive }?.let {
            return it
        }
        return providerScope
            .async {
                runCatching { fetchProviderSnapshot() }
                    .onSuccess {
                        providerSnapshotCache = TimedProviderSnapshot(it, System.nanoTime())
                    }
                    .onFailure { error ->
                        Timber.d(error, "provider node snapshot unavailable")
                    }
                    .getOrElse {
                        providerSnapshotCache?.snapshot ?: ProviderSnapshot.Empty
                    }
            }
            .also { providerSnapshotJob = it }
    }

    private suspend fun fetchProviderSnapshot(): ProviderSnapshot {
        val nodes = LinkedHashMap<String, RawProxy>()
        val owners = HashMap<String, String>()
        fetchProvidersResponse(category = "proxies").providers.forEach { (key, entry) ->
            // The core also registers a synthetic "compatible" provider per group plus a reserved
            // one holding every config-declared proxy. They carry nothing `/proxies` does not
            // already have, and treating them as owners would send delay probes for ordinary nodes
            // down the provider endpoint for no reason.
            if (parseVehicleType(entry.vehicleType) == Provider.VehicleType.Compatible) {
                return@forEach
            }
            val providerName = entry.name.ifBlank { key }
            entry.proxies.forEach { proxy ->
                if (proxy.name.isBlank()) return@forEach
                nodes[proxy.name] = proxy
                if (providerName.isNotBlank()) owners[proxy.name] = providerName
            }
        }
        return ProviderSnapshot(nodes = nodes, owners = owners)
    }

    private suspend fun fetchGroups(): List<RawProxy> {
        val raw = request(HttpMethod.Get, "group").bodyAsText()
        return json.decodeFromString<RawGroupResponse>(raw).proxies
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun patchSelectorAsync(group: String, name: String): Boolean =
        try {
            val response =
                request(
                    HttpMethod.Put,
                    "proxies",
                    group,
                    body = SelectBody(name),
                )
            response.status.isSuccess()
        } catch (_: Throwable) { // fault barrier: remote REST call must degrade to "not selected"
            false
        }

    override fun patchSelector(group: String, name: String): Boolean =
        runBlocking(Dispatchers.IO) { patchSelectorAsync(group, name) }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun closeConnectionAsync(id: String): Boolean =
        try {
            request(HttpMethod.Delete, "connections", id).status.isSuccess()
        } catch (_: Throwable) { // fault barrier: remote REST call must degrade to "not closed"
            false
        }

    override fun closeConnection(id: String): Boolean =
        runBlocking(Dispatchers.IO) { closeConnectionAsync(id) }

    override suspend fun closeAllConnectionsAsync() {
        runCatching { request(HttpMethod.Delete, "connections") }
    }

    override fun closeAllConnections() {
        runBlocking(Dispatchers.IO) { closeAllConnectionsAsync() }
    }

    override suspend fun healthCheck(group: String) {
        runCatching {
            request(
                HttpMethod.Get,
                "group",
                group,
                "delay",
                query = delayQuery,
            )
        }
    }

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        readDelay("proxies", proxyName, "delay")?.let {
            return it
        }
        // `/proxies/{name}` resolves against `tunnel.Proxies()`, so a provider-backed node 404s
        // there and would always read as a timeout. Retry through the provider that owns it.
        val owner = providerSnapshot().owners[proxyName] ?: return -1
        return readDelay("providers", "proxies", owner, proxyName, "healthcheck") ?: -1
    }

    /** Delay in ms, or null when the endpoint rejected the probe (unknown node, timeout, error). */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun readDelay(vararg pathSegments: String): Int? =
        try {
            val response = request(HttpMethod.Get, *pathSegments, query = delayQuery)
            if (response.status.isSuccess()) {
                json.decodeFromString<RawDelayResult>(response.bodyAsText()).delay
            } else {
                null
            }
        } catch (_: Throwable) { // fault barrier: remote REST delay test degrades to "no result"
            null
        }

    /** Proxy + rule providers; built-in Compatible entries are omitted. */
    override suspend fun queryProvidersAsync(): ProviderList {
        val proxies = runCatching {
            fetchProviders(category = "proxies", type = Provider.Type.Proxy)
        }
        val rules = runCatching {
            fetchProviders(category = "rules", type = Provider.Type.Rule)
        }
        if (proxies.isFailure && rules.isFailure) {
            throw requireNotNull(proxies.exceptionOrNull()).also { proxyError ->
                rules.exceptionOrNull()?.let(proxyError::addSuppressed)
            }
        }
        return ProviderList(proxies.getOrDefault(emptyList()) + rules.getOrDefault(emptyList()))
    }

    override fun queryProviders(): ProviderList =
        runBlocking(Dispatchers.IO) { queryProvidersAsync() }

    private suspend fun fetchProvidersResponse(category: String): RawProvidersResponse {
        val raw = request(HttpMethod.Get, "providers", category).bodyAsText()
        return json.decodeFromString<RawProvidersResponse>(raw)
    }

    private suspend fun fetchProviders(
        category: String,
        type: Provider.Type,
    ): List<Provider> {
        val response = fetchProvidersResponse(category)
        return response.providers.mapNotNull { (key, entry) ->
            val vehicle = parseVehicleType(entry.vehicleType) ?: return@mapNotNull null
            if (vehicle == Provider.VehicleType.Compatible) return@mapNotNull null
            val name =
                entry.name
                    .ifBlank { key }
                    .ifBlank {
                        return@mapNotNull null
                    }
            Provider(
                name = name,
                type = type,
                vehicleType = vehicle,
                updatedAt = parseUpdatedAtMillis(entry.updatedAt),
                path = "",
            )
        }
    }

    private fun parseVehicleType(raw: String): Provider.VehicleType? =
        when (raw.trim().lowercase()) {
            "http" -> Provider.VehicleType.HTTP
            "file" -> Provider.VehicleType.File
            "inline" -> Provider.VehicleType.Inline
            "compatible" -> Provider.VehicleType.Compatible
            else -> null
        }

    private fun parseUpdatedAtMillis(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
    }

    override suspend fun updateProvider(type: Provider.Type, name: String) {
        val category = if (type == Provider.Type.Proxy) "proxies" else "rules"
        request(HttpMethod.Put, "providers", category, name)
    }

    override suspend fun queryConfigurationAsync(): UiConfiguration = UiConfiguration()

    override fun queryConfiguration(): UiConfiguration = UiConfiguration()

    override suspend fun queryRulesAsync(): List<RuntimeRule> = fetchRules()

    override fun queryRules(): List<RuntimeRule> = runBlocking(Dispatchers.IO) { queryRulesAsync() }

    /** Toggle a rule, then re-fetch `/rules`. */
    override suspend fun setRuleDisabled(
        rule: RuntimeRule,
        disabled: Boolean,
    ): List<RuntimeRule> {
        request(
            HttpMethod.Patch,
            "rules",
            "disable",
            body = jsonBody(mapOf(rule.index.toString() to disabled)),
        )
        return fetchRules()
    }

    private suspend fun fetchRules(): List<RuntimeRule> {
        val raw = request(HttpMethod.Get, "rules").bodyAsText()
        return json.decodeFromString<RawRulesResponse>(raw).rules.map { it.toRuntimeRule() }
    }

    override fun requestStop() {
        // No-op: we don't own the remote core, so there is nothing to stop.
    }

    override fun subscribeLogs(observer: com.github.yumeyucca.yumebox.runtime.api.LogObserver):
        com.github.yumeyucca.yumebox.runtime.api.LogSubscription = logStream.subscribe(observer)

    private fun RawRule.toRuntimeRule(): RuntimeRule =
        RuntimeRule(
            index = index,
            type = type,
            payload = payload,
            proxy = proxy,
            size = size,
            disabled = extra?.disabled ?: false,
            hitCount = extra?.hitCount ?: 0L,
            missCount = extra?.missCount ?: 0L,
        )

    private fun RawProxy.toProxy(): Proxy =
        Proxy(
            name = name,
            title = name,
            subtitle = "",
            type = type,
            delay = history.lastOrNull()?.delay ?: 0,
        )

    private fun buildGroup(
        group: RawProxy,
        nodes: Map<String, RawProxy>,
        sort: ProxySort,
    ): ProxyGroup {
        val members =
            group.all.map { memberName ->
                nodes[memberName]?.toProxy()
                    ?: Proxy(
                        name = memberName,
                        title = memberName,
                        subtitle = "",
                        type = Proxy.Type.Unknown,
                        delay = 0,
                    )
            }
        val sorted =
            when (sort) {
                ProxySort.Default -> members
                ProxySort.Title -> members.sortedBy { it.name }
                ProxySort.Delay ->
                    members.sortedWith(
                        compareBy(
                            { if (it.delay > 0) 0 else 1 },
                            { if (it.delay > 0) it.delay else Int.MAX_VALUE },
                        )
                    )
            }
        return ProxyGroup(
            name = group.name,
            type = group.type,
            proxies = sorted,
            now = group.now,
            icon = group.icon,
            hidden = group.hidden,
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000L
        const val REQUEST_TIMEOUT_MS = 10_000L
        const val LOCAL_GROUP_QUERY_TIMEOUT_MS = 1_000L
        const val TRAFFIC_SAMPLE_CACHE_NS = 500_000_000L

        const val PROVIDER_SNAPSHOT_WAIT_MS = 400L
        const val PROVIDER_SNAPSHOT_CACHE_NS = 5_000_000_000L
        const val LOCAL_BASE_URL = "http://localhost"

        val delayQuery =
            mapOf(
                "url" to "https://www.gstatic.com/generate_204",
                "timeout" to "5000",
            )
        val LOG_QUERY = mapOf("level" to "debug")
        val CONNECTIONS_QUERY = mapOf("interval" to "1000")
    }
}
