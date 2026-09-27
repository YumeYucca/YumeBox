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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.screen.navigation


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgePlus
import dev.chrisbanes.haze.hazeSource
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Reset
import java.util.*

object EditorDataHolder {
    var listEditorTitle: String = ""
    var listEditorPlaceholder: String = ""
    var listEditorItems: MutableList<String> = mutableListOf()
    var listEditorCallback: ((List<String>?) -> Unit)? = null

    var mapEditorTitle: String = ""
    var mapEditorKeyPlaceholder: String = ""
    var mapEditorValuePlaceholder: String = ""
    var mapEditorItems: MutableMap<String, String> = mutableMapOf()
    var mapEditorCallback: ((Map<String, String>?) -> Unit)? = null

    fun setupListEditor(
        title: String,
        placeholder: String,
        items: List<String>?,
        callback: (List<String>?) -> Unit,
    ) {
        listEditorTitle = title
        listEditorPlaceholder = placeholder
        listEditorItems = items?.toMutableList() ?: mutableListOf()
        listEditorCallback = callback
    }

    fun setupMapEditor(
        title: String,
        keyPlaceholder: String,
        valuePlaceholder: String,
        items: Map<String, String>?,
        callback: (Map<String, String>?) -> Unit,
    ) {
        mapEditorTitle = title
        mapEditorKeyPlaceholder = keyPlaceholder
        mapEditorValuePlaceholder = valuePlaceholder
        mapEditorItems = items?.toMutableMap() ?: mutableMapOf()
        mapEditorCallback = callback
    }

    fun clearListEditor() {
        listEditorTitle = ""
        listEditorPlaceholder = ""
        listEditorItems = mutableListOf()
        listEditorCallback = null
    }

    fun clearMapEditor() {
        mapEditorTitle = ""
        mapEditorKeyPlaceholder = ""
        mapEditorValuePlaceholder = ""
        mapEditorItems = mutableMapOf()
        mapEditorCallback = null
    }
}

private data class TextDraftItem(
    val id: String,
    val value: String,
)

private data class KeyValueDraftItem(
    val id: String,
    val key: String,
    val value: String,
)

private sealed interface StringListDialogState {
    data object None : StringListDialogState

    data object Add : StringListDialogState

    data class Edit(val itemId: String) : StringListDialogState

    data object Reset : StringListDialogState

    data object AddRule : StringListDialogState
}

private sealed interface KeyValueDialogState {
    data object None : KeyValueDialogState

    data object Add : KeyValueDialogState

    data class Edit(val itemId: String) : KeyValueDialogState

    data object Reset : KeyValueDialogState
}

@Composable
fun StringListEditorScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current
    val items = remember { mutableStateListOf<TextDraftItem>() }
    val title = EditorDataHolder.listEditorTitle
    val placeholder = EditorDataHolder.listEditorPlaceholder
    val isOverrideRuleEditor = title == YumeTxt.Override.Label.RulesReplace
    var dialogState by remember {
        mutableStateOf<StringListDialogState>(StringListDialogState.None)
    }

    LaunchedEffect(title, placeholder) {
        items.clear()
        items.addAll(
            EditorDataHolder.listEditorItems.map { value ->
                TextDraftItem(id = UUID.randomUUID().toString(), value = value)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.listEditorCallback?.invoke(
                items.map(TextDraftItem::value).ifEmpty { null }
            )
            EditorDataHolder.clearListEditor()
        }
    }

    val listState = rememberLazyListState()
    EditorScaffold(
        title = title,
        scrollBehavior = scrollBehavior,
        actions =
            listOf(
                EditorAction(
                    icon = MiuixIcons.Reset,
                    contentDescription = YumeTxt.Component.Editor.Dialog.ResetTitle,
                    onClick = { dialogState = StringListDialogState.Reset },
                ),
                EditorAction(
                    icon = Yume.BadgePlus,
                    contentDescription = YumeTxt.Component.Editor.Dialog.AddTitle,
                    onClick = {
                        dialogState =
                            if (isOverrideRuleEditor) {
                                StringListDialogState.AddRule
                            } else {
                                StringListDialogState.Add
                            }
                    },
                ),
            ),
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
        if (items.isEmpty()) {
            EditorEmptyState(
                title = YumeTxt.Component.Editor.Empty.Title,
                hint = YumeTxt.Component.Editor.Empty.Hint,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .let { mod ->
                            if (topBarHazeState != null) mod.hazeSource(topBarHazeState) else mod
                        }
                        .padding(combinedInnerPadding),
            )
        } else {
            ScreenLazyColumn(
                lazyListState = listState,
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Title(YumeTxt.Component.Editor.CountItems.format(items.size))
                }
                items(items = items, key = { it.id }) { item ->
                    val index =
                        remember(items, item.id) { items.indexOfFirst { it.id == item.id } + 1 }
                    EditorListItem(
                        index = index,
                        title = item.value,
                        onClick = { dialogState = StringListDialogState.Edit(item.id) },
                        onDelete = { items.removeAll { it.id == item.id } },
                        deleteIcon = MiuixIcons.Delete,
                        deleteContentDescription = "Delete",
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        StringListDialogState.None -> Unit
        StringListDialogState.Add -> {
            SimpleTextEditorDialog(
                title = YumeTxt.Component.Editor.Dialog.AddTitle,
                placeholder = placeholder,
                initialValue = "",
                onDismiss = { dialogState = StringListDialogState.None },
                onConfirm = { value ->
                    items.add(TextDraftItem(UUID.randomUUID().toString(), value))
                    dialogState = StringListDialogState.None
                },
            )
        }

        is StringListDialogState.Edit -> {
            val currentItem = items.firstOrNull { it.id == state.itemId }
            if (currentItem != null) {
                SimpleTextEditorDialog(
                    title = YumeTxt.Component.Editor.Dialog.EditTitle,
                    placeholder = placeholder,
                    initialValue = currentItem.value,
                    onDismiss = { dialogState = StringListDialogState.None },
                    onConfirm = { value ->
                        val index = items.indexOfFirst { it.id == state.itemId }
                        if (index >= 0) {
                            items[index] = items[index].copy(value = value)
                        }
                        dialogState = StringListDialogState.None
                    },
                )
            } else {
                dialogState = StringListDialogState.None
            }
        }

        StringListDialogState.Reset -> {
            AppConfirmDialog(
                show = true,
                title = YumeTxt.Component.Editor.Dialog.ResetTitle,
                message = YumeTxt.Component.Editor.Dialog.ResetMessage,
                onDismissRequest = { dialogState = StringListDialogState.None },
                onConfirm = {
                    dialogState = StringListDialogState.None
                    EditorDataHolder.listEditorCallback?.invoke(null)
                    EditorDataHolder.clearListEditor()
                    navigator.pop()
                },
            )
        }

        StringListDialogState.AddRule -> {
            RuleEditorDialog(
                title = YumeTxt.Component.Editor.Dialog.AddTitle,
                onDismiss = { dialogState = StringListDialogState.None },
                onConfirm = { value ->
                    items.add(TextDraftItem(UUID.randomUUID().toString(), value))
                    dialogState = StringListDialogState.None
                },
            )
        }
    }
}

@Composable
fun KeyValueEditorScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val topBarHazeState = LocalTopBarHazeState.current
    val items = remember { mutableStateListOf<KeyValueDraftItem>() }
    val title = EditorDataHolder.mapEditorTitle
    val keyPlaceholder = EditorDataHolder.mapEditorKeyPlaceholder
    val valuePlaceholder = EditorDataHolder.mapEditorValuePlaceholder
    var dialogState by remember { mutableStateOf<KeyValueDialogState>(KeyValueDialogState.None) }

    LaunchedEffect(title, keyPlaceholder, valuePlaceholder) {
        items.clear()
        items.addAll(
            EditorDataHolder.mapEditorItems.map { (key, value) ->
                KeyValueDraftItem(id = UUID.randomUUID().toString(), key = key, value = value)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            EditorDataHolder.mapEditorCallback?.invoke(
                items.associate { it.key to it.value }.ifEmpty { null }
            )
            EditorDataHolder.clearMapEditor()
        }
    }

    val listState = rememberLazyListState()
    EditorScaffold(
        title = title,
        scrollBehavior = scrollBehavior,
        actions =
            listOf(
                EditorAction(
                    icon = MiuixIcons.Reset,
                    contentDescription = YumeTxt.Component.Editor.Dialog.ResetTitle,
                    onClick = { dialogState = KeyValueDialogState.Reset },
                ),
                EditorAction(
                    icon = MiuixIcons.AddCircle,
                    contentDescription = YumeTxt.Component.Editor.Dialog.AddTitle,
                    onClick = { dialogState = KeyValueDialogState.Add },
                ),
            ),
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        val combinedInnerPadding = combinePaddingValues(innerPadding, mainLikePadding)
        if (items.isEmpty()) {
            EditorEmptyState(
                title = YumeTxt.Component.Editor.Empty.Title,
                hint = YumeTxt.Component.Editor.Empty.Hint,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .let { mod ->
                            if (topBarHazeState != null) mod.hazeSource(topBarHazeState) else mod
                        }
                        .padding(combinedInnerPadding),
            )
        } else {
            ScreenLazyColumn(
                lazyListState = listState,
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Title(YumeTxt.Component.Editor.CountItems.format(items.size))
                }
                items(items = items, key = { it.id }) { item ->
                    val index =
                        remember(items, item.id) { items.indexOfFirst { it.id == item.id } + 1 }
                    EditorListItem(
                        index = index,
                        title = item.key,
                        summary = item.value,
                        onClick = { dialogState = KeyValueDialogState.Edit(item.id) },
                        onDelete = { items.removeAll { it.id == item.id } },
                        deleteIcon = MiuixIcons.Delete,
                        deleteContentDescription = "Delete",
                    )
                }
            }
        }
    }

    when (val state = dialogState) {
        KeyValueDialogState.None -> Unit
        KeyValueDialogState.Add -> {
            KeyValueFormDialog(
                title = YumeTxt.Component.Editor.Dialog.AddTitle,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                existingKeys = items.map(KeyValueDraftItem::key).toSet(),
                initialKey = "",
                initialValue = "",
                onDismiss = { dialogState = KeyValueDialogState.None },
                onConfirm = { key, value ->
                    items.add(KeyValueDraftItem(UUID.randomUUID().toString(), key, value))
                    dialogState = KeyValueDialogState.None
                },
            )
        }

        is KeyValueDialogState.Edit -> {
            val currentItem = items.firstOrNull { it.id == state.itemId }
            if (currentItem != null) {
                KeyValueFormDialog(
                    title = YumeTxt.Component.Editor.Dialog.EditTitle,
                    keyPlaceholder = keyPlaceholder,
                    valuePlaceholder = valuePlaceholder,
                    existingKeys = items.map(KeyValueDraftItem::key).toSet(),
                    currentEditingKey = currentItem.key,
                    initialKey = currentItem.key,
                    initialValue = currentItem.value,
                    onDismiss = { dialogState = KeyValueDialogState.None },
                    onConfirm = { key, value ->
                        val index = items.indexOfFirst { it.id == state.itemId }
                        if (index >= 0) {
                            items[index] = items[index].copy(key = key, value = value)
                        }
                        dialogState = KeyValueDialogState.None
                    },
                )
            } else {
                dialogState = KeyValueDialogState.None
            }
        }

        KeyValueDialogState.Reset -> {
            AppConfirmDialog(
                show = true,
                title = YumeTxt.Component.Editor.Dialog.ResetTitle,
                message = YumeTxt.Component.Editor.Dialog.ResetMessage,
                onDismissRequest = { dialogState = KeyValueDialogState.None },
                onConfirm = {
                    dialogState = KeyValueDialogState.None
                    EditorDataHolder.mapEditorCallback?.invoke(null)
                    EditorDataHolder.clearMapEditor()
                    navigator.pop()
                },
            )
        }
    }
}
