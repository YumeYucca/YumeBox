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


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.GeoFileType
import com.github.yumeyucca.yumebox.core.model.GeoXItem
import com.github.yumeyucca.yumebox.core.model.geoXItems
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.substore.util.SubStoreDownloadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import java.io.File

@Composable
fun MetaFeatureScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadClient: SubStoreDownloadClient = koinInject()

    val showGeoXDownloadSheet = remember { mutableStateOf(false) }
    val ageKeyHybrid = remember { mutableStateOf(false) }
    val ageKeyDialogVisible = remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(title = YumeTxt.MetaFeature.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.MetaFeature.Section.ConnectionAndTraffic)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.Connection.Title,
                        onClick = { navigator.push(Route.Connection) },
                    )
                    ArrowPreference(
                        title = YumeTxt.Log.Title,
                        onClick = { navigator.push(Route.Log) },
                    )
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.RuntimeRules.Title,
                        onClick = { navigator.push(Route.Rules) },
                    )
                }
            }
            item {
                Title(YumeTxt.MetaFeature.Section.Routing)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.TrafficStatistics.Title,
                        onClick = { navigator.push(Route.TrafficStatistics) },
                    )
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.CustomRouting.Title,
                        onClick = { navigator.push(Route.CustomRouting) },
                    )
                    ArrowPreference(
                        title = YumeTxt.MetaFeature.GeoX.OnlineUpdateTitle,
                        onClick = { showGeoXDownloadSheet.value = true },
                    )
                }
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

        GeoXDownloadDialog(
            show = showGeoXDownloadSheet,
            context = context,
            scope = scope,
            downloadClient = downloadClient,
        )

        AgeKeyGeneratorDialog(
            show = ageKeyDialogVisible.value,
            hybrid = ageKeyHybrid.value,
            onDismiss = { ageKeyDialogVisible.value = false },
            onDismissFinished = {},
        )
    }
}

@Composable
private fun GeoXDownloadDialog(
    show: MutableState<Boolean>,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    downloadClient: SubStoreDownloadClient,
) {
    val spacing = AppTheme.spacing
    val selectedItems = remember { mutableStateMapOf<GeoFileType, Boolean>() }
    val canConfirm = selectedItems.values.any { it }

    AppDialog(
        show = show.value,
        title = YumeTxt.MetaFeature.Download.DialogTitle,
        onDismissRequest = { show.value = false },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                geoXItems.forEach { item ->
                    BasicComponent(
                        title = item.title,
                        endActions = {
                            Checkbox(
                                state = ToggleableState(selectedItems[item.type] ?: false),
                                onClick = {
                                    selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                                },
                            )
                        },
                        onClick = {
                            selectedItems[item.type] = !(selectedItems[item.type] ?: false)
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space16),
            ) {
                TextButton(
                    text = YumeTxt.Component.Button.Cancel,
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = YumeTxt.Component.Button.Confirm,
                    onClick = {
                        val itemsToDownload = geoXItems.filter { selectedItems[it.type] == true }
                        if (itemsToDownload.isEmpty()) {
                            return@TextButton
                        }
                        show.value = false
                        downloadGeoXFiles(context, scope, downloadClient, itemsToDownload)
                    },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

private fun downloadGeoXFiles(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    downloadClient: SubStoreDownloadClient,
    items: List<GeoXItem>,
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
        context.toast(
            YumeTxt.MetaFeature.Download.DownloadComplete.format(successCount, items.size)
        )
    }
}
