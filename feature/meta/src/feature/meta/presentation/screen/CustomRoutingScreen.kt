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

package com.github.yumeyucca.yumebox.feature.meta.presentation.screen


import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.*
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.CustomRoutingViewModel
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun CustomRoutingScreen(
    onNavigateBack: () -> Unit,
    onOpenYamlEditor: (title: String, content: String, onSave: suspend (String) -> Unit) -> Unit,
) {
    val viewModel: CustomRoutingViewModel = koinViewModel()
    val presetSelection by viewModel.presetSelection.collectAsState()
    val customRoutingContent by viewModel.customRoutingContent.collectAsState()
    val templateRoundTripSafe by viewModel.templateRoundTripSafe.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedUrlTestRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val selectedFallbackRegions = remember { mutableStateListOf<OverridePresetRegion>() }
    val enabledItems = remember { mutableStateListOf<OverridePresetItem>() }
    var enableUrlTestGroup by remember { mutableStateOf(true) }
    var enableFallbackGroup by remember { mutableStateOf(false) }
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    fun applyPresetSelection(selection: OverridePresetTemplateSelection) {
        selectedUrlTestRegions.clear()
        selectedUrlTestRegions.addAll(sortPresetRegions(selection.urlTestRegions))
        selectedFallbackRegions.clear()
        selectedFallbackRegions.addAll(sortPresetRegions(selection.fallbackRegions))
        enabledItems.clear()
        enabledItems.addAll(sortPresetItems(selection.enabledItems))
        enableUrlTestGroup = selection.enableUrlTestGroup
        enableFallbackGroup = selection.enableFallbackGroup
        isDirty = false
    }

    fun editedPresetSelection() =
        OverridePresetTemplateSelection(
            urlTestRegions = selectedUrlTestRegions.toSet(),
            fallbackRegions = selectedFallbackRegions.toSet(),
            enabledItems = enabledItems.toSet(),
            enableUrlTestGroup = enableUrlTestGroup,
            enableFallbackGroup = enableFallbackGroup,
        )

    LaunchedEffect(presetSelection) { applyPresetSelection(presetSelection) }

    fun saveAndExit() {
        if (isSaving) return
        if (!isDirty) {
            onNavigateBack()
            return
        }
        if (!templateRoundTripSafe) {
            applyPresetSelection(presetSelection)
            context.toast(YumeTxt.MetaFeature.CustomRouting.ManualYamlPresetDiscarded)
            onNavigateBack()
            return
        }

        val updatedSelection = editedPresetSelection()
        scope.launch {
            isSaving = true
            viewModel
                .savePresetSelection(updatedSelection)
                .onSuccess {
                    isDirty = false
                    onNavigateBack()
                }
                .onFailure { error ->
                    context.toast(error.message ?: YumeTxt.Override.Save.Failed)
                }
            isSaving = false
        }
    }

    BackHandler { saveAndExit() }

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.MetaFeature.CustomRouting.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            fun openEditor(content: String) {
                                onOpenYamlEditor(
                                    YumeTxt.MetaFeature.CustomRouting.EditYaml,
                                    content,
                                ) { updatedContent ->
                                    viewModel
                                        .saveCustomRoutingYaml(updatedContent)
                                        .getOrElse { throw it }
                                }
                            }

                            when {
                                !isDirty -> openEditor(customRoutingContent)
                                !templateRoundTripSafe -> {
                                    applyPresetSelection(presetSelection)
                                    context.toast(
                                        YumeTxt.MetaFeature.CustomRouting.ManualYamlPresetDiscarded
                                    )
                                    openEditor(customRoutingContent)
                                }

                                else ->
                                    scope.launch {
                                        isSaving = true
                                        viewModel
                                            .savePresetSelection(editedPresetSelection())
                                            .onSuccess {
                                                isDirty = false
                                                openEditor(viewModel.customRoutingContent.value)
                                            }
                                            .onFailure { error ->
                                                context.toast(
                                                    error.message ?: YumeTxt.Override.Save.Failed
                                                )
                                            }
                                        isSaving = false
                                    }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Yume.Edit,
                            contentDescription = YumeTxt.MetaFeature.CustomRouting.EditYaml,
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        val mainPadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainPadding),
        ) {
            item(key = "group-type") {
                RoutingSwitchCard(
                    title = YumeTxt.MetaFeature.CustomRouting.GroupTypeTitle,
                    items = listOf("urltest", "fallback"),
                    iconUrl = ::presetGroupTypeIconUrl,
                    itemTitle = { type ->
                        if (type == "urltest") {
                            YumeTxt.MetaFeature.CustomRouting.GroupTypeUrlTest
                        } else {
                            YumeTxt.MetaFeature.CustomRouting.GroupTypeFallback
                        }
                    },
                    isChecked = { type ->
                        if (type == "urltest") enableUrlTestGroup else enableFallbackGroup
                    },
                    onCheckedChange = { type, checked ->
                        if (type == "urltest") {
                            enableUrlTestGroup = checked
                        } else {
                            enableFallbackGroup = checked
                        }
                        isDirty = true
                    },
                )
            }

            item(key = "urltest-regions") {
                RoutingSwitchCard(
                    title = YumeTxt.MetaFeature.CustomRouting.UrlTestRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    itemTitle = OverridePresetRegion::localizedTitle,
                    isChecked = { region -> region in selectedUrlTestRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedUrlTestRegions, region, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "fallback-regions") {
                RoutingSwitchCard(
                    title = YumeTxt.MetaFeature.CustomRouting.FallbackRegionGroupTitle,
                    items = orderedPresetRegions(),
                    iconUrl = OverridePresetRegion::icon,
                    itemTitle = OverridePresetRegion::localizedTitle,
                    isChecked = { region -> region in selectedFallbackRegions },
                    onCheckedChange = { region, checked ->
                        toggleSelection(selectedFallbackRegions, region, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "base-items") {
                RoutingSwitchCard(
                    title = YumeTxt.Override.Draft.BasicRouting,
                    items = orderedBasePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    itemTitle = OverridePresetItem::localizedTitle,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                )
            }

            item(key = "service-items") {
                RoutingSwitchCard(
                    title = YumeTxt.Override.Draft.ServiceRouting,
                    items = orderedServicePresetItems(),
                    iconUrl = OverridePresetItem::icon,
                    itemTitle = OverridePresetItem::localizedTitle,
                    isChecked = { item -> item in enabledItems },
                    onCheckedChange = { item, checked ->
                        toggleSelection(enabledItems, item, checked)
                        isDirty = true
                    },
                )
            }
        }
    }
}

private fun OverridePresetRegion.localizedTitle(): String =
    when (this) {
        OverridePresetRegion.HK -> YumeTxt.MetaFeature.CustomRouting.Region.HongKong
        OverridePresetRegion.TW -> YumeTxt.MetaFeature.CustomRouting.Region.Taiwan
        OverridePresetRegion.JP -> YumeTxt.MetaFeature.CustomRouting.Region.Japan
        OverridePresetRegion.SG -> YumeTxt.MetaFeature.CustomRouting.Region.Singapore
        OverridePresetRegion.US -> YumeTxt.MetaFeature.CustomRouting.Region.UnitedStates
        OverridePresetRegion.Other -> YumeTxt.MetaFeature.CustomRouting.Region.Other
    }

private fun OverridePresetItem.localizedTitle(): String =
    when (this) {
        OverridePresetItem.Proxy -> YumeTxt.MetaFeature.CustomRouting.Item.Proxy
        OverridePresetItem.Ads -> YumeTxt.MetaFeature.CustomRouting.Item.Ads
        OverridePresetItem.Cn -> YumeTxt.MetaFeature.CustomRouting.Item.China
        OverridePresetItem.GeolocationNotCn -> YumeTxt.MetaFeature.CustomRouting.Item.Global
        OverridePresetItem.Match -> YumeTxt.MetaFeature.CustomRouting.Item.Match
        else -> title
    }

private fun <T> toggleSelection(items: MutableList<T>, item: T, checked: Boolean) {
    if (checked) {
        if (item !in items) {
            items.add(item)
        }
    } else {
        items.remove(item)
    }
}
