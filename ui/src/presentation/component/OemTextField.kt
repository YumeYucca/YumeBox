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

import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val DefaultTextFieldMargin = DpSize(16.dp, 16.dp)
private val DefaultTextFieldRadius = 16.dp

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
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    val hasText = value.text.isNotEmpty()
    val labelVisible = label.isNotEmpty() && !(useLabelAsPlaceholder && hasText)
    val labelFloating = labelVisible && hasText
    val borderWidth by animateDpAsState(if (focused) 2.dp else 0.dp, label = "oem_text_field_border")
    val borderColor by animateColorAsState(
        if (focused) colors.primary else colors.secondaryContainer,
        label = "oem_text_field_border_color",
    )
    val labelOffset by animateDpAsState(
        if (labelFloating) -DefaultTextFieldMargin.height / 2 else 0.dp,
        label = "oem_text_field_label_offset",
    )
    val labelSize by animateDpAsState(
        if (labelFloating) 10.dp else 17.dp,
        label = "oem_text_field_label_size",
    )

    Box(
        modifier =
            modifier
                .squircleBackground(
                    if (containerColor == Color.Unspecified) {
                        colors.secondaryContainer
                    } else {
                        containerColor
                    },
                    DefaultTextFieldRadius,
                )
                .squircleBorder({ borderWidth }, { borderColor }, DefaultTextFieldRadius),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = if (leadingIcon == null) DefaultTextFieldMargin.width else 0.dp,
                            end = if (trailingIcon == null) DefaultTextFieldMargin.width else 0.dp,
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
                            .offset(y = if (labelFloating) DefaultTextFieldMargin.height / 2 else 0.dp),
                    enabled = enabled,
                    readOnly = readOnly,
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
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    OemTextField(
        value = fieldValue,
        onValueChange = { updated ->
            fieldValue = updated
            onValueChange(updated.text)
        },
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = useLabelAsPlaceholder,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        containerColor = containerColor,
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
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    showTopDivider: Boolean = false,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val keyboardController = LocalSoftwareKeyboardController.current
    val valueEndPadding = if (trailingIcon != null) 40.dp else 8.dp
    // The label half is a second, much wider way into the field: a tap there puts the caret in the
    // value instead of doing nothing, which is what made these rows feel unresponsive.
    var fieldView by remember { mutableStateOf<EditText?>(null) }
    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showTopDivider) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier =
                        Modifier
                            .weight(1f)
                            .then(
                                if (onClick == null) {
                                    Modifier.clickable(
                                        enabled = enabled,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        fieldView?.requestFocus()
                                        keyboardController?.show()
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = if (enabled) colors.onSurface else colors.onSurfaceVariantSummary,
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1.6f)
                            .heightIn(min = 56.dp),
                ) {
                    InlineTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                // The value stops short of the card edge instead of hugging it.
                                .padding(start = 8.dp, end = valueEndPadding),
                        enabled = enabled,
                        // A picker row keeps its value legible but hands every tap to the row.
                        readOnly = readOnly || onClick != null,
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
                                    .padding(end = 8.dp)
                        ) {
                            trailingIcon()
                        }
                    }
                }
            }
        }
        if (onClick != null) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        ),
            )
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
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    showTopDivider: Boolean = false,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
) {
    // The platform EditText owns cursor and selection, so the caller's String is mirrored into a
    // TextFieldValue exactly like the String overload of [OemTextField] does.
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    AppTextFieldRow(
        label = label,
        value = fieldValue,
        onValueChange = { updated ->
            fieldValue = updated
            onValueChange(updated.text)
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        trailingIcon = trailingIcon,
        showTopDivider = showTopDivider,
        hint = hint,
        onClick = onClick,
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
                .size(32.dp)
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
            modifier = Modifier.size(20.dp),
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
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    onImeAction: (() -> Unit)?,
    textAlign: TextAlign = TextAlign.Start,
    hint: String? = null,
    onViewCreated: (EditText) -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    PlatformEditText(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        maxLines = 1,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        textColor = colors.onSurface,
        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
        textAlign = textAlign,
        hintText = hint,
        hintColor = colors.onSurfaceVariantSummary,
        onViewCreated = onViewCreated,
    )
}

/** Miuix-compatible search capsule; callers retain ownership of the leading/trailing controls. */
@Composable
fun OemSearchInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    inputModifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onImeAction: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier.background(colors.secondaryContainer, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            PlatformEditText(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).heightIn(min = 45.dp).then(inputModifier),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onImeAction = onImeAction,
                textColor = colors.onSurface,
                textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp),
            )
            trailingIcon?.invoke()
        }
    }
}

@Composable
private fun PlatformEditText(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    onImeAction: (() -> Unit)?,
    textColor: Color,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp),
    textAlign: TextAlign = TextAlign.Start,
    hintText: String? = null,
    hintColor: Color = Color.Unspecified,
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
                if (hintText != null) {
                    setHint(hintText)
                    if (hintColor != Color.Unspecified) setHintTextColor(hintColor.toArgb())
                }
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

                        override fun afterTextChanged(text: android.text.Editable?) {
                            val updatedText = text?.toString().orEmpty()
                            currentOnValueChange(
                                TextFieldValue(
                                    text = updatedText,
                                    selection = TextRange(selectionStart.coerceIn(0, updatedText.length), selectionEnd.coerceIn(0, updatedText.length)),
                                )
                            )
                        }
                    }
                )
            }
        },
        modifier = modifier,
        update = { editText ->
            editText.isEnabled = enabled
            editText.isFocusable = enabled && !readOnly
            editText.isFocusableInTouchMode = enabled && !readOnly
            editText.isCursorVisible = enabled && !readOnly
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
            if (hintText != null && editText.hint?.toString() != hintText) {
                editText.setHint(hintText)
                if (hintColor != Color.Unspecified) editText.setHintTextColor(hintColor.toArgb())
            }
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
