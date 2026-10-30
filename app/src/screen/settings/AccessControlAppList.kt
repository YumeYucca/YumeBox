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
 */

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.rememberAppIconBitmap
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme.spacing
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.accessControlAppItems(
    apps: List<AccessControlViewModel.AppInfo>,
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
) {
    items(items = apps, key = { it.packageName }) { app ->
        AccessControlAppCard(
            app = app,
            selected = app.packageName in uiState.selectedPackages,
            onSelectionChange = { checked ->
                viewModel.onAppSelectionChange(app.packageName, checked)
            },
        )
    }
}

@Composable
private fun AccessControlAppCard(
    app: AccessControlViewModel.AppInfo,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
) {
    val spacing = spacing
    val componentSizes = AppTheme.sizes

    AppCard(modifier = Modifier.padding(vertical = spacing.space4), applyHorizontalPadding = false) {
        BasicComponent(
            insideMargin = PaddingValues(horizontal = spacing.space16, vertical = spacing.space12),
            startAction = {
                AccessControlAppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label,
                    imageSize = componentSizes.iconBadgeMedium,
                    bitmapSize = 80,
                    modifier = Modifier.padding(end = spacing.space12),
                )
            },
            endActions = {
                Checkbox(
                    state = ToggleableState(selected),
                    onClick = { onSelectionChange(!selected) },
                )
            },
            onClick = { onSelectionChange(!selected) },
        ) {
            Text(
                text = app.label,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccessControlAppIcon(
    packageName: String,
    contentDescription: String,
    imageSize: androidx.compose.ui.unit.Dp,
    bitmapSize: Int,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberAppIconBitmap(packageName, bitmapSize) ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier.size(imageSize),
    )
}

@Composable
internal fun AccessControlSearchEmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
