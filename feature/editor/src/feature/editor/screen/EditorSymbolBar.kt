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
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.feature.editor.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class EditorSymbol(
    val display: String,
    val insert: String,
)

// Mirrors the symbol input row from the sora-editor demo.
private val EditorSymbols =
    listOf(
        EditorSymbol(display = "->", insert = "\t"),
        EditorSymbol(display = "{", insert = "{}"),
        EditorSymbol(display = "}", insert = "}"),
        EditorSymbol(display = "(", insert = "("),
        EditorSymbol(display = ")", insert = ")"),
        EditorSymbol(display = ",", insert = ","),
        EditorSymbol(display = ".", insert = "."),
        EditorSymbol(display = ";", insert = ";"),
        EditorSymbol(display = "\"", insert = "\""),
        EditorSymbol(display = "?", insert = "?"),
        EditorSymbol(display = "+", insert = "+"),
        EditorSymbol(display = "-", insert = "-"),
        EditorSymbol(display = "*", insert = "*"),
        EditorSymbol(display = "/", insert = "/"),
        EditorSymbol(display = "<", insert = "<"),
        EditorSymbol(display = ">", insert = ">"),
        EditorSymbol(display = "[", insert = "["),
        EditorSymbol(display = "]", insert = "]"),
        EditorSymbol(display = ":", insert = ":"),
    )

@Composable
internal fun EditorSymbolBar(
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(UiDp.dp48)
                    .background(MiuixTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = UiDp.dp8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
        ) {
            EditorSymbols.forEach { symbol ->
                IconButton(
                    onClick = { onInsert(symbol.insert) },
                    minWidth = UiDp.dp40,
                    minHeight = UiDp.dp40,
                    cornerRadius = UiDp.dp12,
                ) {
                    Text(
                        text = symbol.display,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
