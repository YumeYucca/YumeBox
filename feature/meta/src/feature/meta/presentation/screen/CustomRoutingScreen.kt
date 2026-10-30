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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.*
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.CustomRoutingViewModel
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@Composable
fun CustomRoutingScreen(
    onNavigateBack: () -> Unit,
    onOpenYamlEditor: (title: String, content: String, onSave: suspend (String) -> Unit) -> Unit,
) {
    val viewModel: CustomRoutingViewModel = koinViewModel()
    val presetSelection by viewModel.presetSelection.collectAsState()
    val templateRoundTripSafe by viewModel.templateRoundTripSafe.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // One edited draft instead of four parallel lists: each card reads it and copies it back.
    var selection by remember { mutableStateOf(defaultOverridePresetTemplateSelection()) }
    // Derived, so no toggle path can forget to raise it and none can leave it stuck raised.
    val isDirty = selection != presetSelection
    var isSaving by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    fun persistSelection(onSaved: () -> Unit) {
        if (isSaving) return
        scope.launch {
            isSaving = true
            viewModel
                .savePresetSelection(selection)
                .onSuccess { onSaved() }
                .onFailure { error ->
                    // Leaving the screen cancels the save; that is not a save failure.
                    if (error is CancellationException) throw error
                    context.toast(error.message ?: YumeTxt.Override.Save.Failed)
                }
            isSaving = false
        }
    }

    // The saved preset is the baseline: a new one (save, external reload) resets the draft.
    LaunchedEffect(presetSelection) { selection = presetSelection }

    /**
     * The single "leave with the right draft" policy: run [then] as is when clean, drop edits the
     * YAML cannot carry when a manual edit made the preset unrepresentable, otherwise persist first.
     */
    fun saveThen(then: () -> Unit) {
        if (isSaving) return
        when {
            !isDirty -> then()
            !templateRoundTripSafe -> {
                selection = presetSelection
                context.toast(YumeTxt.MetaFeature.CustomRouting.ManualYamlPresetDiscarded)
                then()
            }
            else -> persistSelection(then)
        }
    }

    BackHandler(enabled = !isSaving) { saveThen(onNavigateBack) }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.MetaFeature.CustomRouting.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            saveThen {
                                onOpenYamlEditor(
                                    YumeTxt.MetaFeature.CustomRouting.EditYaml,
                                    viewModel.customRoutingContent.value,
                                ) { updatedContent ->
                                    viewModel.saveCustomRoutingYaml(updatedContent).getOrThrow()
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

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = paddingValues,
        ) {
            items(switchCardSpecs, key = { spec -> spec.key }) { spec ->
                SwitchCard(
                    spec = spec,
                    selection = selection,
                    onSelectionChange = { updated -> selection = updated },
                )
            }
        }
    }
}

/**
 * One card of the routing editor: its title, its items, and the slice of the edited preset those
 * items toggle.
 *
 * The toggle is a pure `(draft, item, checked) -> draft` function, so all five cards are one list
 * and one loop instead of five near-identical [RoutingSwitchCard] call sites.
 */
@Immutable
private class SwitchCardSpec<T>(
    val key: String,
    val title: () -> String,
    val items: List<T>,
    val iconUrl: (T) -> String?,
    val label: (T) -> String,
    val isChecked: (OverridePresetTemplateSelection, T) -> Boolean,
    val toggle: (OverridePresetTemplateSelection, T, Boolean) -> OverridePresetTemplateSelection,
)

/**
 * The cards in display order.
 *
 * Titles and labels are lambdas: [YumeTxt] values are getters over the active locale, so reading
 * them while building this list would freeze whichever language happened to be active then.
 */
private val switchCardSpecs: List<SwitchCardSpec<*>> =
    listOf(
        SwitchCardSpec(
            key = "group-type",
            title = { YumeTxt.MetaFeature.CustomRouting.GroupTypeTitle },
            items = OfficialMrsHealthCheckGroupType.entries,
            iconUrl = OfficialMrsHealthCheckGroupType::icon,
            label = { type -> type.label() },
            isChecked = { draft, type -> type in draft.healthCheckGroups },
            toggle = { draft, type, checked ->
                draft.copy(healthCheckGroups = draft.healthCheckGroups.withToggled(type, checked))
            },
        ),
        SwitchCardSpec(
            key = "urltest-regions",
            title = { YumeTxt.MetaFeature.CustomRouting.UrlTestRegionGroupTitle },
            items = orderedRegions,
            iconUrl = OverridePresetRegion::icon,
            label = { region -> region.label() },
            isChecked = { draft, region -> region in draft.urlTestRegions },
            toggle = { draft, region, checked ->
                draft.copy(urlTestRegions = draft.urlTestRegions.withToggled(region, checked))
            },
        ),
        SwitchCardSpec(
            key = "fallback-regions",
            title = { YumeTxt.MetaFeature.CustomRouting.FallbackRegionGroupTitle },
            items = orderedRegions,
            iconUrl = OverridePresetRegion::icon,
            label = { region -> region.label() },
            isChecked = { draft, region -> region in draft.fallbackRegions },
            toggle = { draft, region, checked ->
                draft.copy(fallbackRegions = draft.fallbackRegions.withToggled(region, checked))
            },
        ),
        // Base and service routing split the item list, not the selection: both cards edit the same
        // `enabledItems` set, so the partition stays disjoint and complete.
        SwitchCardSpec(
            key = "base-items",
            title = { YumeTxt.Override.Draft.BasicRouting },
            items = orderedBaseItems,
            iconUrl = OverridePresetItem::icon,
            label = { item -> item.label() },
            isChecked = { draft, item -> item in draft.enabledItems },
            toggle = { draft, item, checked ->
                draft.copy(enabledItems = draft.enabledItems.withToggled(item, checked))
            },
        ),
        SwitchCardSpec(
            key = "service-items",
            title = { YumeTxt.Override.Draft.ServiceRouting },
            items = orderedServiceItems,
            iconUrl = OverridePresetItem::icon,
            label = { item -> item.label() },
            isChecked = { draft, item -> item in draft.enabledItems },
            toggle = { draft, item, checked ->
                draft.copy(enabledItems = draft.enabledItems.withToggled(item, checked))
            },
        ),
    )

@Composable
private fun <T> SwitchCard(
    spec: SwitchCardSpec<T>,
    selection: OverridePresetTemplateSelection,
    onSelectionChange: (OverridePresetTemplateSelection) -> Unit,
) {
    RoutingSwitchCard(
        title = spec.title(),
        items = spec.items,
        iconUrl = spec.iconUrl,
        itemTitle = spec.label,
        isChecked = { item -> spec.isChecked(selection, item) },
        onCheckedChange = { item, checked ->
            onSelectionChange(spec.toggle(selection, item, checked))
        },
    )
}

private fun <T> Set<T>.withToggled(item: T, checked: Boolean): Set<T> =
    if (checked) this + item else this - item
