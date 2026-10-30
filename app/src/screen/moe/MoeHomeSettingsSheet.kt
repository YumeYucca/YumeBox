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

/**
 * The part of the home settings the sheet edits as a draft: it is only written back when the
 * user confirms, because expanding the sidebar would slide its own sheet out from under them.
 */
@Immutable
internal data class MoeHomeCopy(
    val quote: String,
    val author: String,
    val expandSidebar: Boolean,
)

@Composable
internal fun MoeHomeSettingsSheet(
    show: Boolean,
    copy: MoeHomeCopy,
    useSystemWallpaper: Boolean,
    statusBarSpacing: Boolean,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onStatusBarSpacingChange: (Boolean) -> Unit,
    onSaveCopy: (MoeHomeCopy) -> Unit,
    onChangeWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = AppTheme.spacing
    // Re-seeded per opening so a cancelled sheet never leaks its edits into the next one.
    var draft by remember(show, copy) { mutableStateOf(copy) }
    val save = {
        onSaveCopy(draft)
        onDismiss()
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.Home.Settings.Title,
        endAction = { AppBottomSheetConfirmAction(onClick = save) },
        onDismissRequest = onDismiss,
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
                        value = draft.quote,
                        onValueChange = { draft = draft.copy(quote = it) },
                    )
                    AppTextFieldRow(
                        label = YumeTxt.Home.Settings.Author,
                        value = draft.author,
                        onValueChange = { draft = draft.copy(author = it) },
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
                        checked = draft.expandSidebar,
                        onCheckedChange = { draft = draft.copy(expandSidebar = it) },
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
