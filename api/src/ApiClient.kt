/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package io.github.yumeyucca.yumebox.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import java.io.Closeable
import java.net.URI
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Connection settings for a controller endpoint. */
data class ApiConfig(
    val endpoint: String,
    val secret: String = "",
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
) {
    init {
        require(connectTimeoutMillis > 0) { "connectTimeoutMillis must be positive" }
        require(requestTimeoutMillis > 0) { "requestTimeoutMillis must be positive" }

        val uri = runCatching { URI(endpoint.trim()) }.getOrNull()
        require(uri?.scheme in setOf("http", "https") && !uri?.host.isNullOrBlank()) {
            "endpoint must be an absolute HTTP(S) URL"
        }
    }

    internal val normalizedEndpoint: String
        get() = endpoint.trim().trimEnd('/')

    public companion object {
        public const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Long = 5_000
        public const val DEFAULT_REQUEST_TIMEOUT_MILLIS: Long = 10_000
    }
}

/** Failure returned by the controller or by the client while handling its response. */
sealed class ApiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** The controller responded with a non-success HTTP status. */
class ApiHttpException(
    val statusCode: Int,
    val responseBody: String,
) : ApiException("controller returned HTTP $statusCode")

/** The controller returned a successful response whose body was not valid JSON. */
class ApiProtocolException(cause: Throwable) : ApiException("controller returned invalid JSON", cause)

/**
 * Kotlin/JVM client for the controller REST API.
 *
 * The client owns its HTTP client unless an [httpClient] is supplied. Call [close] when a
 * long-lived instance is no longer needed. All helpers delegate to [request], so newer
 * endpoints remain available without waiting for this library to add a wrapper.
 */
class ApiClient(
    val config: ApiConfig,
    private val httpClient: HttpClient? = null,
) : Closeable {
    private val ownsHttpClient = httpClient == null
    private val client: HttpClient =
        httpClient
            ?: HttpClient(OkHttp) {
                expectSuccess = false
                install(HttpTimeout) {
                    connectTimeoutMillis = config.connectTimeoutMillis
                    requestTimeoutMillis = config.requestTimeoutMillis
                    socketTimeoutMillis = config.requestTimeoutMillis
                }
            }

    /** JSON codec used by generic [get] and [request] calls. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Calls any controller route and decodes its JSON response.
     *
     * [path] is always appended to [ApiConfig.endpoint]; absolute URLs are not
     * accepted, so credentials cannot accidentally be sent to another host.
     */
    suspend fun request(
        method: HttpMethod,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: JsonElement? = null,
    ): JsonElement {
        val response = requestText(method, path, query, body)
        if (response.isBlank()) return JsonNull
        return try {
            json.parseToJsonElement(response)
        } catch (error: Throwable) {
            throw ApiProtocolException(error)
        }
    }

    /** Calls any controller route and decodes the response through the supplied serializer. */
    suspend fun <T> request(
        method: HttpMethod,
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        body: JsonElement? = null,
    ): T = json.decodeFromJsonElement(serializer, request(method, path, query, body))

    /** Convenience wrapper for a JSON GET request. */
    suspend fun get(path: String, query: Map<String, String> = emptyMap()): JsonElement =
        request(HttpMethod.Get, path, query)

    /** Convenience wrapper for a typed JSON GET request. */
    suspend fun <T> get(
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
    ): T = request(HttpMethod.Get, path, serializer, query)

    /** Raw response variant for line-oriented endpoints such as `/logs`. */
    suspend fun requestText(
        method: HttpMethod,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: JsonElement? = null,
    ): String {
        val response =
            client.request(buildUrl(path, query)) {
                this.method = method
                applyAuthorization()
                body?.let { jsonBody ->
                    setBody(
                        TextContent(
                            json.encodeToString(JsonElement.serializer(), jsonBody),
                            ContentType.Application.Json,
                        )
                    )
                }
            }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw ApiHttpException(response.status.value, text)
        }
        return text
    }

    suspend fun configs(): JsonObject = getObject("configs")

    /** Returns the active mihomo tunnel mode reported by `/configs`. */
    suspend fun tunnelMode(): String =
        configs()["mode"]?.jsonPrimitive?.contentOrNull
            ?: throw ApiProtocolException(IllegalStateException("configs response is missing mode"))

    suspend fun patchConfigs(config: JsonObject): JsonElement =
        request(HttpMethod.Patch, "configs", body = config)

    suspend fun traffic(): JsonObject = getObject("traffic")

    suspend fun proxies(): JsonObject = getObject("proxies")

    suspend fun proxy(name: String): JsonObject = getObject("proxies/$name")

    suspend fun selectProxy(group: String, name: String) {
        request(
            HttpMethod.Put,
            "proxies/$group",
            body = buildJsonObject { put("name", name) },
        )
    }

    suspend fun proxyDelay(
        name: String,
        testUrl: String = DEFAULT_DELAY_TEST_URL,
        timeoutMillis: Long = DEFAULT_DELAY_TIMEOUT_MILLIS,
    ): JsonObject =
        getObject(
            "proxies/$name/delay",
            mapOf("url" to testUrl, "timeout" to timeoutMillis.toString()),
        )

    suspend fun group(name: String): JsonObject = getObject("group/$name")

    suspend fun groupDelay(
        name: String,
        testUrl: String = DEFAULT_DELAY_TEST_URL,
        timeoutMillis: Long = DEFAULT_DELAY_TIMEOUT_MILLIS,
    ): JsonObject =
        getObject(
            "group/$name/delay",
            mapOf("url" to testUrl, "timeout" to timeoutMillis.toString()),
        )

    suspend fun connections(intervalMillis: Long? = null): JsonObject =
        getObject("connections", intervalMillis?.let { mapOf("interval" to it.toString()) } ?: emptyMap())

    suspend fun closeConnection(id: String) {
        request(HttpMethod.Delete, "connections/$id")
    }

    suspend fun closeAllConnections() {
        request(HttpMethod.Delete, "connections")
    }

    suspend fun providers(type: ProviderType): JsonObject = getObject("providers/${type.path}")

    suspend fun updateProvider(type: ProviderType, name: String) {
        request(HttpMethod.Put, "providers/${type.path}/$name")
    }

    suspend fun rules(): JsonObject = getObject("rules")

    suspend fun setRuleDisabled(index: Int, disabled: Boolean): JsonObject =
        requireObject(
            request(
                HttpMethod.Patch,
                "rules/disable",
                body = buildJsonObject { put(index.toString(), disabled) },
            )
        )

    override fun close() {
        if (ownsHttpClient) client.close()
    }

    private suspend fun getObject(path: String, query: Map<String, String> = emptyMap()): JsonObject =
        requireObject(get(path, query))

    private fun buildUrl(path: String, query: Map<String, String>): String {
        require(!path.contains("://")) { "path must be relative to the configured endpoint" }
        return URLBuilder().apply {
            takeFrom(config.normalizedEndpoint)
            appendPathSegments(*path.trim('/').split('/').filter(String::isNotBlank).toTypedArray())
            query.forEach { (key, value) -> parameters.append(key, value) }
        }.buildString()
    }

    private fun HttpRequestBuilder.applyAuthorization() {
        val secret = config.secret.trim()
        if (secret.isNotEmpty()) {
            header(HttpHeaders.Authorization, "Bearer $secret")
        }
    }

    private fun requireObject(value: JsonElement): JsonObject =
        value as? JsonObject
            ?: throw ApiProtocolException(
                IllegalStateException("expected a JSON object but received ${value::class.simpleName}")
            )

    enum class ProviderType(internal val path: String) {
        Proxies("proxies"),
        Rules("rules"),
    }

    public companion object {
        public const val DEFAULT_DELAY_TEST_URL: String = "https://www.gstatic.com/generate_204"
        public const val DEFAULT_DELAY_TIMEOUT_MILLIS: Long = 5_000
    }
}
