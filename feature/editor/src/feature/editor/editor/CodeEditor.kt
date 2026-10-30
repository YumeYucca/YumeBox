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

package com.github.yumeyucca.yumebox.feature.editor.editor

import android.content.Context
import android.util.TypedValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.feature.editor.language.TextMateInitializer
import com.github.yumeyucca.yumebox.feature.editor.theme.EditorThemeManager
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.DragSelectStopEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.HandleStateChangeEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeAlways
import timber.log.Timber

@Suppress("TooGenericExceptionCaught")
@Composable
fun CodeEditor(
    state: CodeEditorState,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = DefaultEditorFontSizeSp,
    onScroll: ((Int) -> Unit)? = null,
    onTextChange: ((String) -> Unit)? = null,
) {
    val editorRef = remember { mutableStateOf<CodeEditor?>(null) }
    val selectionMenuController = remember { SelectionMenuController() }

    DisposableEffect(Unit) {
        onDispose {
            editorRef.value?.let { editor ->
                try {
                    editor.release()
                } catch (error: Exception) { // fault barrier: best-effort Sora editor cleanup
                    Timber.e(error, "Failed to release editor")
                }
            }
            state.editor = null
            editorRef.value = null
            selectionMenuController.clear()
        }
    }

    val editorThemeState = EditorThemeManager.rememberEditorTheme()
    LaunchedEffect(editorRef.value, editorThemeState.isDark, state.language) {
        editorRef.value?.let { editor ->
            EditorThemeManager.updateTheme(editor, editorThemeState.isDark)
            TextMateInitializer.setLanguage(editor, state.language)
            selectionMenuController.dismiss()
        }
    }

    LaunchedEffect(state.content) {
        val editor = editorRef.value
        if (editor != null && editor.text.toString() != state.content) {
            editor.setText(state.content)
        }
    }

    LaunchedEffect(fontSizeSp) {
        editorRef.value?.setTextSize(fontSizeSp)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                createCodeEditor(
                    ctx,
                    state,
                    editorThemeState.isDark,
                    fontSizeSp,
                    onScroll,
                    onTextChange,
                    selectionMenuController::update,
                ).also { editor ->
                    state.editor = editor
                    state.refreshHistoryState()
                    editorRef.value = editor
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { editor ->
                try {
                    editor.release()
                } catch (error: Exception) { // fault barrier: best-effort Sora editor cleanup
                    Timber.e(error, "Failed to release editor in onRelease")
                }
            },
        )

        selectionMenuController.state?.let { menuState ->
            key(menuState) {
                TextSelectionActionsMenu(
                    state = menuState,
                    show = selectionMenuController.isVisible,
                    onDismiss = selectionMenuController::dismiss,
                    onDismissFinished = selectionMenuController::finishDismiss,
                    onAction = { action ->
                        selectionMenuController.dismissForAction()
                        menuState.editor.performSelectionAction(action)
                    },
                )
            }
        }
    }
}

private fun createCodeEditor(
    context: Context,
    state: CodeEditorState,
    isDark: Boolean,
    fontSizeSp: Float,
    onScroll: ((Int) -> Unit)?,
    onTextChange: ((String) -> Unit)?,
    onSelectionMenuChange: (SelectionMenuState?) -> Unit,
): CodeEditor {
    TextMateInitializer.initialize(context)

    TextMateInitializer.setTheme(isDark)

    return IndentGuideCodeEditor(context).apply {
        // The editor has a fixed text size; font size changes go through the bottom bar.
        setTextSize(fontSizeSp)
        isScalable = false
        setTabWidth(if (state.language == LanguageScope.Text) 4 else 2)

        val font = EditorFontManager.getEditorTypeface(context)
        typefaceText = font
        typefaceLineNumber = font

        setLineNumberMarginLeft(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.resources.displayMetrics,
            )
        )
        val dividerMargin =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                context.resources.displayMetrics,
            )
        setDividerMargin(dividerMargin, dividerMargin)

        setScrollBarEnabled(false)
        getComponent(EditorTextActionWindow::class.java).isEnabled = false

        setBlockLineEnabled(true)
        setBlockLineWidth(1f)
        setHighlightCurrentBlock(true)
        setHighlightCurrentLine(true)
        setHighlightBracketPair(true)
        registerInlayHintRenderers(
            TextInlayHintRenderer.DefaultInstance,
            ColorInlayHintRenderer(
                ResolvableColor { scheme ->
                    scheme.getColor(EditorColorScheme.TEXT_NORMAL)
                }
            ),
        )

        nonPrintablePaintingFlags = CodeEditor.FLAG_DRAW_LINE_SEPARATOR

        setText(state.content)

        EditorThemeManager.applyTheme(this, isDark)
        TextMateInitializer.setLanguage(this, state.language)
        EditorThemeManager.updateTheme(this, isDark)
        registerInlayHintProvider(ColorInlayHintProvider(this))

        subscribeAlways<ContentChangeEvent> {
            state.syncContentFromEditor()
            onTextChange?.invoke(state.content)
            invalidateInlayHints()
            onSelectionMenuChange(null)
        }
        subscribeAlways<ScrollEvent> { event ->
            onScroll?.invoke(event.endY)
            onSelectionMenuChange(null)
        }
        subscribeAlways<SelectionChangeEvent> { event ->
            if (!event.isSelected || getEventHandler().isDragSelecting()) {
                onSelectionMenuChange(null)
            } else {
                postInLifecycle {
                    onSelectionMenuChange(buildSelectionMenuState(this))
                }
            }
        }
        subscribeAlways<DragSelectStopEvent> {
            postInLifecycle {
                onSelectionMenuChange(buildSelectionMenuState(this))
            }
        }
        subscribeAlways<HandleStateChangeEvent> { event ->
            if (event.isHeld) {
                onSelectionMenuChange(null)
            } else if (getCursor().isSelected()) {
                postInLifecycle {
                    onSelectionMenuChange(buildSelectionMenuState(this))
                }
            }
        }
        subscribeAlways<EditorFocusChangeEvent> { event ->
            if (!event.isGainFocus) onSelectionMenuChange(null)
        }

        Timber.d("CodeEditor created: language=${state.language}")
    }
}
