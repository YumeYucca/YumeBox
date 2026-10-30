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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.settings


import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.formatDateTime
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.GeoXItem
import com.github.yumeyucca.yumebox.core.model.geoXItems
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.di.APPLICATION_SCOPE_NAME
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.substore.util.SubStoreDownloadClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import java.io.File

private data class GeoFileStatus(
    val item: GeoXItem,
    val lastModified: Long?,
)

/**
 * What the AGE key dialog is doing. [Closed] keeps the last generated key type so the exit tween
 * does not swap the dialog title and reset its fields while it fades out.
 */
private sealed interface AgeKeyDialog {
    val hybrid: Boolean

    data class Closed(override val hybrid: Boolean) : AgeKeyDialog

    data class Open(override val hybrid: Boolean) : AgeKeyDialog
}

@Composable
fun MetaFeatureScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    // Geo downloads must outlive this screen: cancelling an in-flight HttpURLConnection
    // read from the composition scope closes the socket on the wrong thread and crashes.
    val downloadScope: CoroutineScope = koinInject(qualifier = named(APPLICATION_SCOPE_NAME))
    val downloadClient: SubStoreDownloadClient = koinInject()

    var geoRefreshKey by remember { mutableStateOf(0) }
    val runtimeHome = context.runtimeHomeDir
    val geoStatuses by produceState(
        geoXItems.map { GeoFileStatus(it, null) },
        runtimeHome,
        geoRefreshKey,
    ) {
        value =
            withContext(Dispatchers.IO) {
                geoXItems.map { item ->
                    val file = File(runtimeHome, item.fileName)
                    GeoFileStatus(item, if (file.exists()) file.lastModified() else null)
                }
            }
    }
    val lastUpdated = geoStatuses.mapNotNull { it.lastModified }.maxOrNull()
    val lastUpdatedText = remember(lastUpdated) { formatGeoLastUpdated(lastUpdated) }
    var ageKeyDialog by remember {
        mutableStateOf<AgeKeyDialog>(AgeKeyDialog.Closed(hybrid = false))
    }

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Settings.UiSettings.MetaFeatures,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item { MetaNavigationSection(navigator) }
            item { MetaTrafficStatisticsSection(navigator) }
            item {
                GeoXOnlineUpdateSection(
                    context = context,
                    scope = downloadScope,
                    downloadClient = downloadClient,
                    lastUpdatedText = lastUpdatedText,
                    onDownloaded = { geoRefreshKey++ },
                )
            }
            item { AgeKeySection { hybrid -> ageKeyDialog = AgeKeyDialog.Open(hybrid = hybrid) } }
        }

        AgeKeyGeneratorDialog(
            show = ageKeyDialog is AgeKeyDialog.Open,
            hybrid = ageKeyDialog.hybrid,
            onDismiss = { ageKeyDialog = AgeKeyDialog.Closed(hybrid = ageKeyDialog.hybrid) },
        )
    }
}

/** Logs and connection entries. */
@Composable
private fun MetaNavigationSection(navigator: Navigator) {
    Title(YumeTxt.MetaFeature.Section.LogsAndConnections)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.Settings.More.Logs,
            onClick = { navigator.push(Route.Log) },
        )
        PreferenceArrowItem(
            title = YumeTxt.Connection.Title,
            onClick = { navigator.push(Route.Connection) },
        )
    }
    SectionSummary(YumeTxt.MetaFeature.Section.LogsAndConnectionsSummary)
}

/** Entry point to the traffic statistics page. */
@Composable
private fun MetaTrafficStatisticsSection(navigator: Navigator) {
    Title(YumeTxt.TrafficStatistics.Title)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.TrafficStatistics.Title,
            onClick = { navigator.push(Route.TrafficStatistics) },
        )
    }
}

/** GeoX online update row plus the formatted last-update summary under it. */
@Composable
private fun GeoXOnlineUpdateSection(
    context: Context,
    scope: CoroutineScope,
    downloadClient: SubStoreDownloadClient,
    lastUpdatedText: String,
    onDownloaded: () -> Unit,
) {
    Title(YumeTxt.MetaFeature.GeoX.OnlineUpdateTitle)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.MetaFeature.GeoX.OnlineUpdateTitle,
            onClick = {
                downloadGeoXFiles(
                    context = context,
                    scope = scope,
                    downloadClient = downloadClient,
                    items = geoXItems,
                    onComplete = onDownloaded,
                )
            },
        )
    }
    SectionSummary(
        YumeTxt.MetaFeature.GeoX.OnlineUpdateSummary.format(
            lastUpdatedText
        )
    )
}

/** The two AGE key generators; [onGenerate] receives whether the hybrid variant was picked. */
@Composable
private fun AgeKeySection(onGenerate: (Boolean) -> Unit) {
    Title(YumeTxt.MetaFeature.AgeKey.Section)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.MetaFeature.AgeKey.X25519Title,
            onClick = { onGenerate(false) },
        )
        PreferenceArrowItem(
            title = YumeTxt.MetaFeature.AgeKey.HybridTitle,
            onClick = { onGenerate(true) },
        )
    }
}


private fun formatGeoLastUpdated(timestamp: Long?): String =
    if (timestamp == null || timestamp <= 0L) "—" else formatDateTime(timestamp, "yyyy-MM-dd HH:mm")

private fun downloadGeoXFiles(
    context: Context,
    scope: CoroutineScope,
    downloadClient: SubStoreDownloadClient,
    items: List<GeoXItem>,
    onComplete: () -> Unit = {},
) {
    scope.launch {
        var successCount = 0
        withContext(Dispatchers.IO) {
            val runtimeHome = context.runtimeHomeDir
            runtimeHome.mkdirs()
            items.forEach { item ->
                val targetFile = File(runtimeHome, item.fileName)
                if (downloadClient.download(item.url, targetFile)) {
                    successCount++
                }
            }
        }
        onComplete()
        context.toast(
            YumeTxt.MetaFeature.Download.DownloadComplete.format(successCount, items.size)
        )
    }
}
