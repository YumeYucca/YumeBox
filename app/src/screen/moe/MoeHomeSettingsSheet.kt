/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.Palette
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The part of the home settings the sheet edits as a draft: it is only written back when the user confirms, so a
 * half-typed quote is discarded when the sheet is swiped away.
 */
@Immutable
internal data class MoeHomeCopy(
    val quote: String,
    val author: String,
)

/**
 * The home's edit entry point. It only edits the quote inline; settings and wallpaper are separate pages, so the two
 * tiles below navigate instead of toggling anything in place.
 */
@Composable
internal fun MoeHomeSettingsSheet(
    show: Boolean,
    copy: MoeHomeCopy,
    onSaveCopy: (MoeHomeCopy) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = spacing.space16)) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.space12),
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                MoeHomeSettingsTile(
                    icon = Yume.Bolt,
                    title = YumeTxt.Settings.Title,
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
                MoeHomeSettingsTile(
                    icon = Yume.Palette,
                    title = YumeTxt.Home.Settings.Wallpaper,
                    onClick = onOpenWallpaper,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Icon over label, centred: reads as a pair of destinations rather than a list of toggles. */
@Composable
private fun MoeHomeSettingsTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    AppCard(
        modifier = modifier,
        applyHorizontalPadding = false,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.space20),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(spacing.space8))
            Text(text = title, style = MiuixTheme.textStyles.body2)
        }
    }
}
