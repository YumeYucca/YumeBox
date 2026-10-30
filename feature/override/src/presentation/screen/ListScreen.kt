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

package com.github.yumeyucca.yumebox.feature.override.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgePlus
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.feature.override.presentation.viewmodel.OverrideConfigViewModel
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Download
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverrideListScreen(
    onOpenCodeEditor: (OverrideConfig) -> Unit,
    viewModel: OverrideConfigViewModel = koinViewModel(),
) {
    val builtInConfigs by viewModel.builtInConfigs.collectAsState()
    val userConfigs by viewModel.userConfigs.collectAsState()
    val usageCountMap by viewModel.usageCountMap.collectAsState()
    val pendingRevealConfigId by viewModel.pendingRevealConfigId.collectAsState()

    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()

    var dialog by remember { mutableStateOf<OverrideListDialog?>(null) }
    var exportTargetConfig by remember { mutableStateOf<OverrideConfig?>(null) }

    val listState = rememberLazyListState()
    var selectedSectionIndex by rememberSaveable { mutableIntStateOf(0) }
    val builtInItems =
        remember(builtInConfigs, usageCountMap) { builtInConfigs.toListItems(usageCountMap) }
    val userItems =
        remember(userConfigs, usageCountMap) { userConfigs.toListItems(usageCountMap) }
    // One tab row always sits above the cards, so both sections start at index 1.
    val userListIndexOffset = 1
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            if (selectedSectionIndex != 1) return@rememberReorderableLazyListState
            val fromUser = from.index - userListIndexOffset
            val toUser = to.index - userListIndexOffset
            if (fromUser < 0 || toUser < 0) return@rememberReorderableLazyListState
            viewModel.reorderUserConfigs(fromUser, toUser)
        }

    val exportConfigLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            val targetConfig = exportTargetConfig
            if (uri == null || targetConfig == null) {
                exportTargetConfig = null
                return@rememberLauncherForActivityResult
            }

            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(targetConfig.content.toByteArray())
                    output.flush()
                } ?: error(YumeTxt.Util.Error.UnknownError)
            }
                .onSuccess {
                    context.toast(YumeTxt.Override.Export.Success.format(targetConfig.name))
                }
                .onFailure { error ->
                    val reason = error.message ?: YumeTxt.Util.Error.UnknownError
                    context.toast(YumeTxt.Override.Export.Failed.format(reason))
                }

            exportTargetConfig = null
        }

    val exportConfig: (OverrideConfig) -> Unit = { config ->
        exportTargetConfig = config
        exportConfigLauncher.launch("${config.name}.${config.contentType.extension}")
    }

    // Configs can be edited elsewhere (routing presets, the code editor), so the counts and the
    // rule-usage map are re-read whenever this list comes back to the foreground.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Only jump after an explicit create/import/duplicate. Re-run when list data arrives so we
    // don't consume the pending id before the new row is composed.
    LaunchedEffect(
        pendingRevealConfigId,
        builtInItems,
        userItems,
        selectedSectionIndex,
        userListIndexOffset,
    ) {
        val targetId = pendingRevealConfigId ?: return@LaunchedEffect
        val builtInIndex = builtInItems.indexOfFirst { it.config.id == targetId }
        val userIndex = userItems.indexOfFirst { it.config.id == targetId }
        val targetSection =
            when {
                userIndex >= 0 -> 1
                builtInIndex >= 0 -> 0
                else -> return@LaunchedEffect
            }
        if (selectedSectionIndex != targetSection) {
            selectedSectionIndex = targetSection
            return@LaunchedEffect
        }
        val targetIndex =
            userListIndexOffset + if (targetSection == 0) builtInIndex else userIndex
        listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        viewModel.consumePendingRevealConfig(targetId)
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Override.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.BadgePlus,
                                    contentDescription = YumeTxt.Override.Action.Create,
                                    onClick = {
                                        dialog = OverrideListDialog.Create(OverrideConfigInputMode.CreateNew)
                                    },
                                ),
                                GlassTopAction(
                                    icon = Lucide.Download,
                                    contentDescription = YumeTxt.Override.Action.NetworkImport,
                                    onClick = {
                                        dialog = OverrideListDialog.Create(OverrideConfigInputMode.LocalFile)
                                    },
                                ),
                            ),
                        backdrop = LocalTopBarBackdrop.current,
                    )
                },
            )
        },
    ) { paddingValues ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = paddingValues,
            lazyListState = listState,
        ) {
            stickySegmentedTabs(
                key = "__override_sections__",
                tabs =
                    listOf(
                        YumeTxt.Override.Section.BuiltIn,
                        YumeTxt.Override.Section.User,
                    ),
                selectedTabIndex = selectedSectionIndex,
                onTabSelected = { selectedSectionIndex = it },
                tabHeight = UiDp.dp42,
            )

            if (selectedSectionIndex == 0) {
                items(
                    items = builtInItems,
                    key = { it.config.id },
                    contentType = { "override-builtin-card" },
                ) { item ->
                    OverrideCardSlot(
                        item = item,
                        isBuiltIn = true,
                        isDragging = false,
                        onExport = exportConfig,
                        onEdit = onOpenCodeEditor,
                        onRename = { dialog = OverrideListDialog.Rename(it) },
                        onDelete = null,
                    )
                }
            } else {
                items(
                    items = userItems,
                    key = { it.config.id },
                    contentType = { "override-config-card" },
                ) { item ->
                    ReorderableItem(state = reorderState, key = item.config.id) { isDragging ->
                        OverrideCardSlot(
                            item = item,
                            isBuiltIn = false,
                            isDragging = isDragging,
                            onExport = exportConfig,
                            onEdit = onOpenCodeEditor,
                            onRename = { dialog = OverrideListDialog.Rename(it) },
                            onDelete = { viewModel.deleteConfig(it.id) },
                            modifier = Modifier.longPressDraggableHandle(),
                        )
                    }
                }
            }
        }

        val createDialog = dialog as? OverrideListDialog.Create
        CreateConfigDialog(
            show = createDialog != null,
            initialMode = createDialog?.mode ?: OverrideConfigInputMode.CreateNew,
            onConfirmCreate = { name, contentType ->
                viewModel.createConfig(name = name, contentType = contentType)
                dialog = null
            },
            onConfirmImport = { content, sourceName ->
                viewModel.importConfig(content = content, sourceName = sourceName)
            },
            onConfirmNetworkImport = viewModel::importConfigFromUrl,
            onDismiss = { dialog = null },
        )

        val renameDialog = dialog as? OverrideListDialog.Rename
        RenameConfigDialog(
            show = renameDialog != null,
            config = renameDialog?.config,
            onConfirm = { newName ->
                renameDialog?.config?.id?.let { id -> viewModel.renameConfig(id, newName) }
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

/**
 * 这一页同一时间只会有一个弹窗,所以用一个槽位表达,而不是几个互斥的布尔值。
 */
private sealed interface OverrideListDialog {
    data class Create(val mode: OverrideConfigInputMode) : OverrideListDialog

    data class Rename(val config: OverrideConfig) : OverrideListDialog
}

/**
 * 内置表与用户表共用同一张卡,差别只在外面的拖拽壳和有没有删除入口。
 */
@Composable
private fun OverrideCardSlot(
    item: OverrideConfigListItem,
    isBuiltIn: Boolean,
    isDragging: Boolean,
    onExport: (OverrideConfig) -> Unit,
    onEdit: (OverrideConfig) -> Unit,
    onRename: (OverrideConfig) -> Unit,
    onDelete: ((OverrideConfig) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val config = item.config
    OverrideConfigCard(
        config = config,
        isDragging = isDragging,
        isInUse = item.isInUse,
        isBuiltIn = isBuiltIn,
        onExport = { onExport(config) },
        onEdit = { onEdit(config) },
        onRename = { onRename(config) },
        onDelete = onDelete?.let { delete -> { delete(config) } },
        modifier = modifier,
    )
}

private data class OverrideConfigListItem(
    val config: OverrideConfig,
    val isInUse: Boolean,
)

private fun List<OverrideConfig>.toListItems(
    usageCountMap: Map<String, Int>
): List<OverrideConfigListItem> =
    map { config ->
        OverrideConfigListItem(
            config = config,
            isInUse = (usageCountMap[config.id] ?: 0) > 0,
        )
    }
