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

package com.github.yumeyucca.yumebox.screen.settings.component


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.component.EnumSelector
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Palette
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.colorFromArgb
import com.github.yumeyucca.yumebox.presentation.theme.colorToArgbLong
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.Icon

@Composable
internal fun ThemeModeAndColorItems(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
) {
    ThemeModeSelectorItem(themeMode = themeMode, onThemeModeChange = onThemeModeChange)
    ThemeColorPickerItem(
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = onThemeSeedColorChange,
    )
}

@Composable
internal fun ThemeModeSelectorItem(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    EnumSelector(
        title = YumeTxt.AppSettings.Interface.ThemeModeTitle,
        summary = YumeTxt.AppSettings.Interface.ThemeModeSummary,
        currentValue = themeMode,
        items =
            listOf(
                YumeTxt.AppSettings.Interface.ThemeModeSystem,
                YumeTxt.AppSettings.Interface.ThemeModeLight,
                YumeTxt.AppSettings.Interface.ThemeModeDark,
            ),
        values = ThemeMode.entries,
        onValueChange = onThemeModeChange,
    )
}

@Composable
internal fun ThemeColorPickerItem(
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
) {
    ThemeColorPickerItem(
        themeSeedColorArgb = themeSeedColorArgb,
        onThemeSeedColorChange = onThemeSeedColorChange,
        showBottomSheetInPlace = true,
    )
}

@Composable
internal fun ThemeColorPickerItem(
    themeSeedColorArgb: Long,
    onThemeSeedColorChange: (Long) -> Unit,
    showBottomSheetInPlace: Boolean,
    onOpenPickerRequest: (() -> Unit)? = null,
) {
    val showThemeColorPicker = remember { mutableStateOf(false) }

    BasicComponent(
        title = YumeTxt.AppSettings.Interface.ColorThemeTitle,
        summary =
            YumeTxt.AppSettings.Interface.ColorThemeCustomSummary.format(
                formatThemeSeedHex(themeSeedColorArgb)
            ),
        onClick = {
            if (showBottomSheetInPlace) {
                showThemeColorPicker.value = true
            } else {
                onOpenPickerRequest?.invoke()
            }
        },
        endActions = {
            val previewColor =
                remember(themeSeedColorArgb) {
                    runCatching { colorFromArgb(themeSeedColorArgb) }.getOrDefault(Color.White)
                }
            Icon(
                Yume.Palette,
                tint = previewColor,
                contentDescription = null,
                modifier = Modifier.padding(end = UiDp.dp12),
            )
        },
    )

    if (showBottomSheetInPlace) {
        ThemeColorPickerSheet(
            show = showThemeColorPicker.value,
            initialSeedColorArgb = themeSeedColorArgb,
            onDismissRequest = { showThemeColorPicker.value = false },
            onConfirm = { argb ->
                onThemeSeedColorChange(argb)
                showThemeColorPicker.value = false
            },
        )
    }
}

@Composable
internal fun ThemeColorPickerSheet(
    show: Boolean,
    initialSeedColorArgb: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    renderInRootScaffold: Boolean = true,
) {
    val pickerColor =
        remember(show, initialSeedColorArgb) {
            mutableStateOf(
                runCatching { colorFromArgb(initialSeedColorArgb) }.getOrDefault(Color.White)
            )
        }
    val hexField =
        remember(show, initialSeedColorArgb) {
            val hex = formatThemeSeedHex(initialSeedColorArgb)
            mutableStateOf(TextFieldValue(hex, TextRange(hex.length)))
        }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = YumeTxt.AppSettings.Interface.ColorThemePickerTitle,
        onDismissRequest = onDismissRequest,
        enableNestedScroll = true,
        renderInRootScaffold = renderInRootScaffold,
        defaultWindowInsetsPadding = false,
        startAction = { AppBottomSheetCloseAction(onClick = onDismissRequest) },
        endAction = {
            AppBottomSheetConfirmAction(onClick = { onConfirm(colorToArgbLong(pickerColor.value)) })
        },
        content = {
            Column(modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()) {
                ColorPicker(
                    color = pickerColor.value,
                    onColorChanged = { color ->
                        pickerColor.value = color
                        val hex = formatThemeSeedHex(colorToArgbLong(color))
                        hexField.value = TextFieldValue(hex, TextRange(hex.length))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OemTextField(
                    value = hexField.value,
                    onValueChange = { updated ->
                        val upper = updated.copy(text = updated.text.uppercase())
                        hexField.value = upper
                        parseThemeHexColorOrNull(upper.text)?.let { pickerColor.value = it }
                    },
                    label = YumeTxt.AppSettings.Interface.ColorThemeCodeLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = UiDp.dp8),
                )
            }
        },
    )
}

private fun formatThemeSeedHex(argb: Long): String {
    val rgb = (argb and 0x00FFFFFFL).toString(16).uppercase().padStart(6, '0')
    return "#$rgb"
}

private fun parseThemeHexColorOrNull(input: String): Color? {
    val body = input.removePrefix("#").removePrefix("0x").uppercase()
    if (body.length != 6) return null
    val rgb = body.toLongOrNull(16) ?: return null
    return colorFromArgb(0xFF000000L or rgb)
}
