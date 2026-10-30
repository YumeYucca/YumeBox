/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.presentation.component

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DefaultTextFieldMargin = DpSize(UiDp.dp16, UiDp.dp16)
private val DefaultTextFieldRadius = UiDp.dp16

/**
 * Resting size of the [OemTextField] label. The label animation runs in [androidx.compose.ui.unit.Dp]
 * (it feeds `labelSize.value.sp`), shrinks to [UiDp.dp10] while floating, and returns to this value
 * at rest; 17dp has no [UiDp] token, so it stays a named constant instead of a bare literal at the
 * animation site.
 */
private val DefaultLabelRestingFontSize = 17.dp

/**
 * Mirrors an outside [value] into the [TextFieldValue] the platform field edits, keeping the caret
 * at the end of any value that arrives from outside.
 */
@Composable
internal fun rememberMirroredTextFieldValue(value: String): MutableState<TextFieldValue> {
    val state = remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (state.value.text != value) {
            state.value = TextFieldValue(value, TextRange(value.length))
        }
    }
    return state
}

/**
 * Miuix TextField chrome with a platform [EditText] as its input node. Keeping the host view as
 * an EditText is what allows OEM TextView hooks to supply the selection handles and action menu.
 */
@Composable
fun OemTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    val hasText = value.text.isNotEmpty()
    val labelVisible = label.isNotEmpty() && !(useLabelAsPlaceholder && hasText)
    val labelFloating = labelVisible && hasText
    val borderWidth by animateDpAsState(if (focused) UiDp.dp2 else UiDp.dp0, label = "oem_text_field_border")
    val borderColor by animateColorAsState(
        if (focused) colors.primary else colors.secondaryContainer,
        label = "oem_text_field_border_color",
    )
    val labelOffset by animateDpAsState(
        if (labelFloating) -DefaultTextFieldMargin.height / 2 else UiDp.dp0,
        label = "oem_text_field_label_offset",
    )
    val labelSize by animateDpAsState(
        if (labelFloating) UiDp.dp10 else DefaultLabelRestingFontSize,
        label = "oem_text_field_label_size",
    )

    Box(
        modifier =
            modifier
                .squircleBackground(colors.secondaryContainer, DefaultTextFieldRadius)
                .squircleBorder({ borderWidth }, { borderColor }, DefaultTextFieldRadius),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = DefaultTextFieldMargin.width,
                            end = if (trailingIcon == null) DefaultTextFieldMargin.width else UiDp.dp0,
                            top = DefaultTextFieldMargin.height,
                            bottom = DefaultTextFieldMargin.height,
                        ),
                contentAlignment = Alignment.TopStart,
            ) {
                if (labelVisible) {
                    Text(
                        text = label,
                        fontSize = labelSize.value.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSecondaryContainer,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.offset(y = labelOffset),
                    )
                }
                PlatformEditText(
                    value = value,
                    onValueChange = onValueChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .offset(y = if (labelFloating) DefaultTextFieldMargin.height / 2 else UiDp.dp0),
                    enabled = enabled,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    onImeAction = onImeAction,
                    textColor = colors.onSurface,
                    onFocusChanged = { focused = it },
                )
            }
            trailingIcon?.invoke()
        }
    }
}

@Composable
fun OemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val fieldValue = rememberMirroredTextFieldValue(value)
    OemTextField(
        value = fieldValue.value,
        onValueChange = { updated ->
            fieldValue.value = updated
            onValueChange(updated.text)
        },
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = useLabelAsPlaceholder,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        trailingIcon = trailingIcon,
    )
}

/**
 * Sheet form row: a persistent label on the leading side and a borderless value that starts at the
 * leading edge of its half, separated from the row above by an optional hairline — the shape of the
 * reference import sheet (名称 / 链接 / age 私钥 in one card).
 *
 * The value never wraps: a long link runs to the card edge and is clipped there (it scrolls under
 * the caret while editing) instead of pushing the row into two lines and losing the label/value
 * rhythm. [hint] is the faint right-aligned tag an empty optional row carries ("可选"), rendered
 * beside the empty value so it does not force the text itself to the right.
 *
 * The field itself is as tall as the row (56dp), so a tap anywhere in the value half lands on the
 * caret instead of a thin strip of text at the vertical centre. [trailingIcon] hosts a per-row
 * control (the age key's eye).
 */
@Composable
fun AppTextFieldRow(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    showTopDivider: Boolean = false,
    hint: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    val keyboardController = LocalSoftwareKeyboardController.current
    val valueEndPadding = if (trailingIcon != null) UiDp.dp40 else UiDp.dp8
    // The label half is a second, much wider way into the field: a tap there puts the caret in the
    // value instead of doing nothing, which is what made these rows feel unresponsive.
    var fieldView by remember { mutableStateOf<EditText?>(null) }
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = UiDp.dp16))
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = UiDp.dp56)
                    .padding(start = UiDp.dp16, end = UiDp.dp16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            fieldView?.requestFocus()
                            keyboardController?.show()
                        },
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = if (enabled) colors.onSurface else colors.onSurfaceVariantSummary,
            )
            Box(
                modifier =
                    Modifier
                        .weight(1.6f)
                        .heightIn(min = UiDp.dp56),
            ) {
                InlineTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = UiDp.dp56)
                            // The value stops short of the card edge instead of hugging it.
                            .padding(start = UiDp.dp8, end = valueEndPadding),
                    enabled = enabled,
                    keyboardOptions = keyboardOptions,
                    onImeAction = onImeAction,
                    textAlign = TextAlign.End,
                    onViewCreated = { fieldView = it },
                )
                if (value.text.isEmpty() && hint != null) {
                    Text(
                        text = hint,
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = valueEndPadding),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                if (trailingIcon != null) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = UiDp.dp8)
                    ) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

@Composable
fun AppTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    showTopDivider: Boolean = false,
    hint: String? = null,
) {
    // The platform EditText owns cursor and selection, so the caller's String is mirrored into a
    // TextFieldValue exactly like the String overload of [OemTextField] does.
    val fieldValue = rememberMirroredTextFieldValue(value)
    AppTextFieldRow(
        label = label,
        value = fieldValue.value,
        onValueChange = { updated ->
            fieldValue.value = updated
            onValueChange(updated.text)
        },
        modifier = modifier,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        trailingIcon = trailingIcon,
        showTopDivider = showTopDivider,
        hint = hint,
    )
}

/**
 * Trailing control for [AppTextFieldRow] — the age key's eye: 32dp tap target, 20dp glyph, no
 * ripple chrome so it reads as part of the row.
 */
@Composable
fun AppTextFieldRowIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Box(
        modifier =
            modifier
                .size(UiDp.dp32)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(UiDp.dp20),
            tint =
                if (tint == Color.Unspecified) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    tint
                },
        )
    }
}

/** Borderless single-line field for [AppTextFieldRow]: no container, no focus rim. */
@Composable
private fun InlineTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    onImeAction: (() -> Unit)?,
    textAlign: TextAlign = TextAlign.Start,
    onViewCreated: (EditText) -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    PlatformEditText(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        maxLines = 1,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        textColor = colors.onSurface,
        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
        textAlign = textAlign,
        onViewCreated = onViewCreated,
    )
}

@Composable
private fun PlatformEditText(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    singleLine: Boolean,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    onImeAction: (() -> Unit)?,
    textColor: Color,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp),
    textAlign: TextAlign = TextAlign.Start,
    onFocusChanged: (Boolean) -> Unit = {},
    onViewCreated: (EditText) -> Unit = {},
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnImeAction by rememberUpdatedState(onImeAction)
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)

    AndroidView(
        factory = { context ->
            EditText(context).apply {
                background = null
                setPadding(0, 0, 0, 0)
                includeFontPadding = false
                gravity = Gravity.CENTER_VERTICAL or textAlign.horizontalGravity()
                isSingleLine = singleLine
                setMaxLines(maxLines)
                inputType = keyboardOptions.keyboardType.toInputType(singleLine)
                imeOptions = keyboardOptions.imeAction.toEditorInfoAction()
                setTextColor(textColor.toArgb())
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textStyle.fontSize.value)
                setText(value.text)
                setSelection(value.selection.end.coerceIn(0, value.text.length))
                onViewCreated(this)
                setOnFocusChangeListener { _, hasFocus -> currentOnFocusChanged(hasFocus) }
                setOnEditorActionListener { _, _, _ ->
                    currentOnImeAction?.invoke()
                    currentOnImeAction != null
                }
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

                        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(text: Editable?) {
                            val updatedText = text?.toString().orEmpty()
                            val caret = selectionStart.coerceIn(0, updatedText.length) to
                                selectionEnd.coerceIn(0, updatedText.length)
                            currentOnValueChange(
                                TextFieldValue(
                                    text = updatedText,
                                    selection = TextRange(caret.first, caret.second),
                                ),
                            )
                        }
                    }
                )
            }
        },
        modifier = modifier,
        update = { editText ->
            editText.isEnabled = enabled
            editText.isFocusable = enabled
            editText.isFocusableInTouchMode = enabled
            editText.isCursorVisible = enabled
            val inputType = keyboardOptions.keyboardType.toInputType(singleLine)
            if (editText.inputType != inputType) {
                // Changing the input type (the age key's eye toggles password/plain) rebuilds the
                // transformation and drops the caret to the start; put it back where it was.
                val selectionStart = editText.selectionStart
                val selectionEnd = editText.selectionEnd
                editText.inputType = inputType
                if (selectionStart >= 0 && selectionEnd >= 0) {
                    val length = editText.text.length
                    editText.setSelection(
                        selectionStart.coerceIn(0, length),
                        selectionEnd.coerceIn(0, length),
                    )
                }
            }
            if (editText.isSingleLine != singleLine) editText.isSingleLine = singleLine
            if (editText.maxLines != maxLines) editText.maxLines = maxLines
            val imeOptions = keyboardOptions.imeAction.toEditorInfoAction()
            if (editText.imeOptions != imeOptions) editText.imeOptions = imeOptions
            val gravity = Gravity.CENTER_VERTICAL or textAlign.horizontalGravity()
            if (editText.gravity != gravity) editText.gravity = gravity
            editText.setTextColor(textColor.toArgb())
            editText.setOnFocusChangeListener { _, hasFocus -> currentOnFocusChanged(hasFocus) }
            editText.setOnEditorActionListener { _, _, _ ->
                currentOnImeAction?.invoke()
                currentOnImeAction != null
            }
            if (editText.text.toString() != value.text) editText.setText(value.text)
            if (!editText.hasFocus()) editText.setSelection(value.selection.end.coerceIn(0, value.text.length))
        },
    )
}

private fun TextAlign.horizontalGravity(): Int =
    when (this) {
        TextAlign.End, TextAlign.Right -> Gravity.END
        TextAlign.Center -> Gravity.CENTER_HORIZONTAL
        else -> Gravity.START
    }

private fun KeyboardType.toInputType(singleLine: Boolean): Int {
    val textFlags = if (singleLine) 0 else InputType.TYPE_TEXT_FLAG_MULTI_LINE
    return when (this) {
        KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
        KeyboardType.Decimal -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
        KeyboardType.Uri -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        KeyboardType.NumberPassword -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> InputType.TYPE_CLASS_TEXT or textFlags
    }
}

private fun ImeAction.toEditorInfoAction(): Int =
    when (this) {
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        ImeAction.None -> EditorInfo.IME_ACTION_NONE
        else -> EditorInfo.IME_ACTION_UNSPECIFIED
    }
