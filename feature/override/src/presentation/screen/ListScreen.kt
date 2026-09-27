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

@file:Suppress("FunctionName", "RedundantIf", "UnnecessaryVariable")

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgePlus
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

private data class OverrideListVmState(
    val builtInConfigs: List<OverrideConfig>,
    val userConfigs: List<OverrideConfig>,
    val usageCountMap: Map<String, Int>,
    val pendingRevealConfigId: String?,
)

@Composable
private fun rememberOverrideListVmState(viewModel: OverrideConfigViewModel): OverrideListVmState {
    val builtInConfigs by viewModel.builtInConfigs.collectAsState()
    val userConfigs by viewModel.userConfigs.collectAsState()
    val usageCountMap by viewModel.usageCountMap.collectAsState()
    val pendingRevealConfigId by viewModel.pendingRevealConfigId.collectAsState()
    return remember(builtInConfigs, userConfigs, usageCountMap, pendingRevealConfigId) {
        OverrideListVmState(
            builtInConfigs = builtInConfigs,
            userConfigs = userConfigs,
            usageCountMap = usageCountMap,
            pendingRevealConfigId = pendingRevealConfigId,
        )
    }
}

private class OverrideListDialogState {
    val showCreateDialog = mutableStateOf(false)
    var createDialogMode by mutableStateOf(OverrideConfigInputMode.CreateNew)
    val showDeleteDialog = mutableStateOf(false)
    val deleteTargetConfig = mutableStateOf<OverrideConfig?>(null)
    val exportTargetConfig = mutableStateOf<OverrideConfig?>(null)
    val applyTargetConfig = mutableStateOf<OverrideConfig?>(null)
}

@Composable
private fun rememberOverrideListDialogState(): OverrideListDialogState = remember {
    OverrideListDialogState()
}

@Composable
fun OverrideListScreen(
    onOpenCodeEditor: (OverrideConfig) -> Unit,
    viewModel: OverrideConfigViewModel = koinViewModel(),
) {
    val vmState = rememberOverrideListVmState(viewModel)
    val builtInConfigs = vmState.builtInConfigs
    val userConfigs = vmState.userConfigs
    val usageCountMap = vmState.usageCountMap
    val pendingRevealConfigId = vmState.pendingRevealConfigId

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = MiuixScrollBehavior()

    val dialogs = rememberOverrideListDialogState()
    val showCreateDialog = dialogs.showCreateDialog
    var createDialogMode by dialogs::createDialogMode
    val showDeleteDialog = dialogs.showDeleteDialog
    val deleteTargetConfig = dialogs.deleteTargetConfig
    val exportTargetConfig = dialogs.exportTargetConfig
    val applyTargetConfig = dialogs.applyTargetConfig

    val listState = rememberLazyListState()
    val createFabController = rememberOverrideFabController()
    val builtInItems =
        remember(builtInConfigs, usageCountMap) {
            builtInConfigs.map { config ->
                OverrideConfigListItem(
                    config = config,
                    isInUse = (usageCountMap[config.id] ?: 0) > 0,
                    isBuiltIn = true,
                )
            }
        }
    val userItems =
        remember(userConfigs, usageCountMap) {
            userConfigs.map { config ->
                OverrideConfigListItem(
                    config = config,
                    isInUse = (usageCountMap[config.id] ?: 0) > 0,
                    isBuiltIn = false,
                )
            }
        }
    // Reorder only applies to the user section; list indices must skip built-in rows + titles.
    // Layout: [builtin title?] + N built-in cards + [user title?] + M user cards
    val userListIndexOffset =
        remember(builtInItems, userItems) {
            val builtinBlock = if (builtInItems.isEmpty()) 0 else 1 + builtInItems.size
            val userTitle = if (userItems.isEmpty()) 0 else 1
            builtinBlock + userTitle
        }
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromUser = from.index - userListIndexOffset
            val toUser = to.index - userListIndexOffset
            if (fromUser < 0 || toUser < 0) return@rememberReorderableLazyListState
            viewModel.reorderUserConfigs(fromUser, toUser)
        }

    val exportConfigLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            val targetConfig = exportTargetConfig.value
            if (uri == null || targetConfig == null) {
                exportTargetConfig.value = null
                return@rememberLauncherForActivityResult
            }

            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(targetConfig.content.toByteArray())
                    output.flush()
                } ?: error(YumeTxt.Override.Export.Failed.format(targetConfig.name))
            }
                .onSuccess {
                    context.toast(YumeTxt.Override.Export.Success.format(targetConfig.name))
                }
                .onFailure { error ->
                    context.toast(YumeTxt.Override.Export.Failed.format(error.message))
                }

            exportTargetConfig.value = null
        }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Only jump after an explicit create/import/duplicate. Re-run when list data arrives so we
    // don't consume the pending id before the new row is composed.
    LaunchedEffect(pendingRevealConfigId, builtInItems, userItems, userListIndexOffset) {
        val targetId = pendingRevealConfigId ?: return@LaunchedEffect
        val builtInIndex = builtInItems.indexOfFirst { it.config.id == targetId }
        val userIndex = userItems.indexOfFirst { it.config.id == targetId }
        val targetIndex =
            when {
                builtInIndex >= 0 -> (if (builtInItems.isEmpty()) 0 else 1) + builtInIndex

                userIndex >= 0 -> userListIndexOffset + userIndex
                else -> return@LaunchedEffect
            }
        listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        viewModel.consumePendingRevealConfig(targetId)
    }

    Scaffold(
        floatingActionButton = {
            OverrideAnimatedFab(
                controller = createFabController,
                visible = !showCreateDialog.value,
                imageVector = Yume.BadgePlus,
                contentDescription = YumeTxt.Override.Action.Create,
                onClick = {
                    createDialogMode = OverrideConfigInputMode.CreateNew
                    showCreateDialog.value = true
                },
            )
        },
        topBar = { TopBar(title = YumeTxt.Override.Title, scrollBehavior = scrollBehavior) },
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = listState,
            onScrollDirectionChanged = createFabController::onScrollDirectionChanged,
        ) {
            // Built-ins first. No empty-state placeholder — with bundled overrides the list is
            // never blank.
            if (builtInItems.isNotEmpty()) {
                item(key = "section-builtin", contentType = "section-title") {
                    Title(YumeTxt.Override.Section.BuiltIn)
                }
                items(
                    items = builtInItems,
                    key = { it.config.id },
                    contentType = { "override-builtin-card" },
                ) { item ->
                    val config = item.config
                    OverrideConfigCard(
                        config = config,
                        isDragging = false,
                        isInUse = item.isInUse,
                        isBuiltIn = true,
                        onApply = { applyTargetConfig.value = config },
                        onExport = {
                            exportTargetConfig.value = config
                            exportConfigLauncher.launch(
                                "${config.name}.${config.contentType.extension}"
                            )
                        },
                        onEdit = { onOpenCodeEditor(config) },
                        onDelete = null,
                        enableDrag = false,
                    )
                }
            }

            // "导入覆写" section only appears when the user actually has imports — no empty title/hint.
            if (userItems.isNotEmpty()) {
                item(key = "section-user", contentType = "section-title") {
                    Title(YumeTxt.Override.Section.User)
                }
                items(
                    items = userItems,
                    key = { it.config.id },
                    contentType = { "override-config-card" },
                ) { item ->
                    val config = item.config
                    ReorderableItem(state = reorderState, key = config.id) { isDragging ->
                        OverrideConfigCard(
                            config = config,
                            isDragging = isDragging,
                            isInUse = item.isInUse,
                            isBuiltIn = false,
                            onApply = { applyTargetConfig.value = config },
                            onExport = {
                                exportTargetConfig.value = config
                                exportConfigLauncher.launch(
                                    "${config.name}.${config.contentType.extension}"
                                )
                            },
                            onEdit = { onOpenCodeEditor(config) },
                            onDelete = {
                                deleteTargetConfig.value = config
                                showDeleteDialog.value = true
                            },
                            enableDrag = true,
                        )
                    }
                }
            }
        }

        CreateConfigDialog(
            show = showCreateDialog,
            initialMode = createDialogMode,
            onConfirmCreate = { name, contentType ->
                viewModel.createConfig(name = name, contentType = contentType)
                showCreateDialog.value = false
            },
            onConfirmImport = viewModel::importConfig,
            onConfirmNetworkImport = viewModel::importConfigFromUrl,
            onDismiss = { showCreateDialog.value = false },
        )

        DeleteConfirmDialog(
            show = showDeleteDialog,
            config = deleteTargetConfig.value,
            viewModel = viewModel,
            onConfirm = {
                deleteTargetConfig.value?.id?.let(viewModel::deleteConfig)
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
            onDismiss = {
                deleteTargetConfig.value = null
                showDeleteDialog.value = false
            },
        )

        OverrideApplyToProfilesSheet(
            target = applyTargetConfig.value,
            viewModel = viewModel,
            onDismiss = { applyTargetConfig.value = null },
        )
    }
}

private data class OverrideConfigListItem(
    val config: OverrideConfig,
    val isInUse: Boolean,
    val isBuiltIn: Boolean = false,
)
