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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.markdown.MarkdownContent
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Update/changelog dialog with a GitHub-flavored Markdown body and an optional download state.
 *
 * The caller supplies the title and action labels so the update flow can keep its own wording;
 * the body is capped and scrolls internally for long changelogs. While [busy] is true the body
 * swaps to the same refresh indicator the app uses elsewhere, with [busyText] underneath.
 */
@Composable
fun AppMarkdownDialog(
    show: Boolean,
    title: String,
    markdown: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = YumeTxt.Component.Button.Confirm,
    dismissText: String = YumeTxt.Component.Button.Cancel,
    busy: Boolean = false,
    busyText: String? = null,
) {
    AppDialog(
        show = show,
        title = title,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
    ) {
        AppDialogColumn(modifier = Modifier.animateContentSize()) {
            if (busy) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = UiDp.dp24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
                ) {
                    InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp24))
                    busyText?.let { text ->
                        Text(
                            text = text,
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiDp.dp320),
                ) {
                    MarkdownContent(
                        content = markdown,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DialogButtonRow(
                    onCancel = onDismissRequest,
                    onConfirm = onConfirm,
                    cancelText = dismissText,
                    confirmText = confirmText,
                )
            }
        }
    }
}
