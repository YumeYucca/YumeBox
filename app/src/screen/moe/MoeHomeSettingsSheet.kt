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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

@Composable
internal fun MoeHomeSettingsSheet(
    show: Boolean,
    quote: String,
    author: String,
    sidebarExpanded: Boolean,
    useSystemWallpaper: Boolean,
    statusBarSpacing: Boolean,
    onQuoteChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onSidebarExpandedChange: (Boolean) -> Unit,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onStatusBarSpacingChange: (Boolean) -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = AppTheme.spacing
    var draftQuote by remember(show, quote) { mutableStateOf(quote) }
    var draftAuthor by remember(show, author) { mutableStateOf(author) }
    var draftSidebarExpanded by remember(show, sidebarExpanded) { mutableStateOf(sidebarExpanded) }
    val save = {
        onQuoteChange(draftQuote)
        onAuthorChange(draftAuthor)
        onSidebarExpandedChange(draftSidebarExpanded)
        onDismiss()
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.Home.Settings.Title,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
        endAction = { AppBottomSheetConfirmAction(onClick = save) },
        onDismissRequest = onDismiss,
        enableNestedScroll = true,
        // The sheet scrolls this column itself, so a form taller than the sheet (or half-covered by
        // the IME) stays reachable.
        scrollableContent = true,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.space16),
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                applyHorizontalPadding = false,
            ) {
                Column {
                    // Label + value rows instead of placeholder-labelled fields: the copy stays
                    // readable once text is typed, and the value sits on the card surface.
                    AppTextFieldRow(
                        label = YumeTxt.Home.Settings.Quote,
                        value = draftQuote,
                        onValueChange = { draftQuote = it },
                    )
                    AppTextFieldRow(
                        label = YumeTxt.Home.Settings.Author,
                        value = draftAuthor,
                        onValueChange = { draftAuthor = it },
                        showTopDivider = true,
                    )
                }
            }
            AppCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space12),
                applyHorizontalPadding = false,
            ) {
                Column {
                    PreferenceSwitchItem(
                        title = YumeTxt.Home.Settings.ExpandSidebar,
                        checked = draftSidebarExpanded,
                        onCheckedChange = { draftSidebarExpanded = it },
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.Home.Settings.StatusBarSpacing,
                        checked = statusBarSpacing,
                        onCheckedChange = onStatusBarSpacingChange,
                    )
                    SystemWallpaperPreferenceItem(
                        checked = useSystemWallpaper,
                        onCheckedChange = onUseSystemWallpaperChange,
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.Home.Settings.ChangeWallpaper,
                        onClick = onChangeWallpaper,
                    )
                }
            }
        }
    }
}
