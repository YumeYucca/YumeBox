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


import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.GeoXItem
import com.github.yumeyucca.yumebox.core.model.geoXItems
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.substore.util.SubStoreDownloadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.preference.ArrowPreference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class GeoFileStatus(
    val item: GeoXItem,
    val lastModified: Long?,
)

@Composable
fun MetaFeatureScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    val ageKeyHybrid = remember { mutableStateOf(false) }
    val ageKeyDialogVisible = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Settings.UiSettings.MetaFeatures,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.MetaFeature.Section.StatisticsAndLogs)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.TrafficStatistics.Title,
                        onClick = { navigator.push(Route.TrafficStatistics) },
                    )
                    ArrowPreference(
                        title = YumeTxt.Settings.More.Logs,
                        onClick = { navigator.push(Route.Log) },
                    )
                }
                SectionSummary(YumeTxt.MetaFeature.Section.StatisticsAndLogsSummary)
            }
            item {
                Title(YumeTxt.MetaFeature.GeoX.OnlineUpdateTitle)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.GeoX.OnlineUpdateTitle,
                        onClick = {
                            downloadGeoXFiles(
                                context = context,
                                scope = scope,
                                downloadClient = downloadClient,
                                items = geoXItems,
                                onComplete = { geoRefreshKey++ },
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
            item {
                Title(YumeTxt.MetaFeature.AgeKey.Section)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.AgeKey.X25519Title,
                        onClick = {
                            ageKeyHybrid.value = false
                            ageKeyDialogVisible.value = true
                        },
                    )
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.AgeKey.HybridTitle,
                        onClick = {
                            ageKeyHybrid.value = true
                            ageKeyDialogVisible.value = true
                        },
                    )
                }
            }
        }

        AgeKeyGeneratorDialog(
            show = ageKeyDialogVisible.value,
            hybrid = ageKeyHybrid.value,
            onDismiss = { ageKeyDialogVisible.value = false },
            onDismissFinished = {},
        )
    }
}


private fun formatGeoLastUpdated(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun downloadGeoXFiles(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
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
