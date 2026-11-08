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

package com.github.yumeyucca.yumebox.runtime.service.profile

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.FetchStatus
import com.github.yumeyucca.yumebox.runtime.api.ProviderPrefetchReport
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.util.*
import timber.log.Timber

/**
 * Pre-fetches HTTP providers into the same profile-private paths that liboverride emits into the runtime config.
 * Best-effort: one failed provider must never reject an import.
 */
internal suspend fun fetchExternalProviders(
    context: Context,
    uuid: UUID,
    stagingDir: File,
    profileDir: File,
    ageSecretKey: String?,
    onStatus: (FetchStatus) -> Unit,
): ProviderPrefetchReport {
    return runCatching {
            fetchExternalProvidersInner(
                context = context,
                uuid = uuid,
                stagingDir = stagingDir,
                profileDir = profileDir,
                ageSecretKey = ageSecretKey,
                onStatus = onStatus,
            )
        }
        .onFailure { error -> Timber.e(error, "External provider prefetch crashed; continuing import") }
        .getOrDefault(ProviderPrefetchReport())
}

private suspend fun fetchExternalProvidersInner(
    context: Context,
    uuid: UUID,
    stagingDir: File,
    profileDir: File,
    ageSecretKey: String?,
    onStatus: (FetchStatus) -> Unit,
): ProviderPrefetchReport {
    val config = stagingDir.resolve("config.yaml")
    if (!config.isFile || config.length() <= 0L) {
        Timber.e("Skip external provider prefetch: missing config.yaml under %s", stagingDir)
        return ProviderPrefetchReport()
    }

    val configText = readConfigText(config)
    onStatus(
        FetchStatus(
            action = FetchStatus.Action.FetchProviders,
            args = emptyList(),
            progress = 0,
            max = 1,
        )
    )

    // Source config is the source of truth for *which* providers to download. liboverride is
    // only used to obtain the rewritten path that runtime will later resolve.
    val (rewrittenPaths, pathMapFallback) =
        loadLiboverrideProviderPaths(
            context = context,
            uuid = uuid,
            stagingDir = stagingDir,
            profileDir = profileDir,
            ageSecretKey = ageSecretKey,
        )
    if (pathMapFallback) {
        Timber.i("Provider path map=source-fallback for profile %s", uuid)
    }

    val discovery =
        collectDownloadableProviders(
            configText = configText,
            stagingDir = stagingDir,
            profileDir = profileDir,
            rewrittenPaths = rewrittenPaths,
            pathMapSourceFallback = pathMapFallback,
        )
    val providers = discovery.providers

    if (providers.isEmpty()) {
        val declaresProviders = configText.contains("proxy-providers") || configText.contains("rule-providers")
        val hasHttpUrl = HTTP_URL_IN_TEXT.containsMatchIn(configText)
        val anomaly = declaresProviders && hasHttpUrl
        if (anomaly) {
            Timber.e(
                "Provider prefetch collected 0 items but config declares providers with http(s) urls " +
                    "(staging=%s size=%d rewritten=%d pathMapFallback=%s)",
                stagingDir,
                config.length(),
                rewrittenPaths.size,
                pathMapFallback,
            )
        } else {
            Timber.i(
                "No downloadable external providers under %s (declaresProviders=%s hasHttpUrl=%s)",
                stagingDir,
                declaresProviders,
                hasHttpUrl,
            )
        }
        return ProviderPrefetchReport(
            attempted = 0,
            failedNames = emptyList(),
            discoveryAnomaly = anomaly,
            headerDegraded = false,
        )
    }

    Timber.i(
        "Prefetching %d external providers staging=%s profileDir=%s rewritten=%d textScan=%s pathMapFallback=%s",
        providers.size,
        stagingDir,
        profileDir,
        rewrittenPaths.size,
        discovery.usedTextScan,
        pathMapFallback,
    )

    val failedNames = mutableListOf<String>()
    HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
            }
            followRedirects = true
        }
        .use { client ->
            providers.forEachIndexed { index, provider ->
                onStatus(
                    FetchStatus(
                        action = FetchStatus.Action.FetchProviders,
                        args = listOf(provider.name),
                        progress = index + 1,
                        max = providers.size,
                    )
                )
                runCatching { downloadExternalProvider(client, provider) }
                    .onSuccess {
                        Timber.i(
                            "Downloaded provider %s -> %s (%d bytes)",
                            provider.name,
                            provider.target,
                            provider.target.length(),
                        )
                    }
                    .onFailure { error ->
                        failedNames += provider.name
                        Timber.e(
                            error,
                            "Provider download failed: name=%s url=%s target=%s",
                            provider.name,
                            provider.url,
                            provider.target,
                        )
                    }
            }
        }

    // Final progress tick for UI: surface a short failed-name list without blocking import.
    if (failedNames.isNotEmpty()) {
        onStatus(
            FetchStatus(
                action = FetchStatus.Action.FetchProviders,
                args = failedNames.take(3),
                progress = providers.size,
                max = providers.size,
            )
        )
    }

    return ProviderPrefetchReport(
        attempted = providers.size,
        failedNames = failedNames.toList(),
        discoveryAnomaly = false,
        headerDegraded = discovery.usedTextScan,
    )
}

private suspend fun downloadExternalProvider(client: HttpClient, provider: ExternalProvider) {
    val temporary = File(provider.target.parentFile, ".${provider.target.name}.download")
    provider.target.parentFile?.mkdirs()
    temporary.delete()
    try {
        client
            .get(provider.url) {
                header(HttpHeaders.UserAgent, SubscriptionUserAgent.resolve())
                provider.headers.forEach { (name, value) -> header(name, value) }
            }
            .let { response ->
                check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
                response.bodyAsChannel().toInputStream().use { input ->
                    temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                }
            }
        temporary.copyTo(provider.target, overwrite = true)
    } finally {
        temporary.delete()
    }
}
