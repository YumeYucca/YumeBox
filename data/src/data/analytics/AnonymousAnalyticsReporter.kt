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

package com.github.yumeyucca.yumebox.data.analytics

import android.os.Build
import com.tencent.mmkv.MMKV
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Sends one best-effort anonymous launch event per app process.
 *
 * The random UUID is generated locally and HMAC-hashed by the Worker. No hardware identifier,
 * profile, subscription, traffic, or region data is included.
 */
class AnonymousAnalyticsReporter(
    private val mmkv: MMKV,
    private val endpoint: String,
    private val appVersion: String,
    private val versionCode: Int,
) {
    private val baseUrl = endpoint.trim().trimEnd('/')
    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }
        }
    }

    suspend fun reportLaunch() {
        if (baseUrl.isBlank()) return
        withContext(Dispatchers.IO) {
            val deviceId =
                mmkv.decodeString(KEY_DEVICE_ID)
                    ?: UUID.randomUUID().toString().also { mmkv.encode(KEY_DEVICE_ID, it) }
            val payload =
                LaunchEvent(
                    schemaVersion = 1,
                    deviceId = deviceId,
                    androidVersion = Build.VERSION.RELEASE.ifBlank { "unknown" },
                    manufacturer = Build.MANUFACTURER.trim().ifBlank { "Unknown" },
                    appVersion = appVersion,
                    versionCode = versionCode,
                    event = "app_start",
                )
            try {
                val response =
                    client.post("$baseUrl/api/v1/events") {
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                if (!response.status.isSuccess()) {
                    Timber.w("Anonymous analytics returned HTTP ${response.status.value}")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Anonymous analytics report failed")
            }
        }
    }

    private companion object {
        const val KEY_DEVICE_ID = "analytics_device_id"
    }
}

@Serializable
private data class LaunchEvent(
    val schemaVersion: Int,
    val deviceId: String,
    val androidVersion: String,
    val manufacturer: String,
    val appVersion: String,
    val versionCode: Int,
    val event: String,
)

