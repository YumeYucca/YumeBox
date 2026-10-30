/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.CircleFadingArrowUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.Delete
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.icon.yume.Share
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.FileBracesCorner
import io.github.yumeyucca.lucide.lucide.FileSymlink
import io.github.yumeyucca.lucide.lucide.Link
import io.github.yumeyucca.lucide.lucide.ToggleRight
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor

/**
 * What the card's menu may offer right now.
 *
 * These three always travel together — they describe one profile's state of affairs — and keeping
 * them in one type means the menu decides nothing about the profile and the card derives nothing
 * about the menu.
 */
@Immutable
internal data class ProfileActionsAvailability(
    val isActive: Boolean,
    val isDownloading: Boolean,
    val canUpdate: Boolean,
) {
    /** Rows that mutate the profile wait for an import to finish: it owns the same files. */
    val rowsEnabled: Boolean get() = !isDownloading
}

/**
 * One row the card's menu can run. Sealed rather than a lambda per row so the card hands the
 * whole menu back through a single exhaustive `when`, and the menu owns nothing but presentation.
 *
 * Enabling and disabling is deliberately not a row here: it is the only action whose caller-
 * visible effect is a state (`enabled`) rather than a command, so it travels on its own callback
 * instead of as a payload-less case the caller would have to re-derive.
 */
sealed interface ProfileAction {
    data object Edit : ProfileAction

    data object OverrideSettings : ProfileAction

    data object Update : ProfileAction

    data object ShareFile : ProfileAction

    data object ShareLink : ProfileAction

    data object Delete : ProfileAction
}

@Composable
internal fun ProfileActionsMenu(
    show: Boolean,
    availability: ProfileActionsAvailability,
    canOverride: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onToggleRequest: () -> Unit,
    onAction: (ProfileAction) -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
) {
    val strings = YumeTxt.Component.ProfileCard
    val share = rememberGlassSubmenuState(show)

    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
        onDismissFinished = onDismissFinished,
        submenu = share,
        submenuContent = {
            GlassSubmenuPageHeader(state = share, text = strings.Share, icon = Yume.Share, anchor = anchor)
            GlassMenuItem(
                text = strings.ShareFile,
                icon = Lucide.FileSymlink,
                enabled = availability.rowsEnabled,
                onClick = {
                    onDismiss()
                    onAction(ProfileAction.ShareFile)
                },
            )
            GlassMenuItem(
                text = strings.ShareLink,
                icon = Lucide.Link,
                enabled = availability.rowsEnabled && availability.canUpdate,
                onClick = {
                    onDismiss()
                    onAction(ProfileAction.ShareLink)
                },
            )
        },
    ) {
        GlassMenuItem(
            text = if (availability.isActive) strings.Disable else strings.Enable,
            icon = Lucide.ToggleRight,
            enabled = availability.rowsEnabled,
            onClick = {
                onDismiss()
                onToggleRequest()
            },
        )
        GlassMenuItem(
            text = strings.Edit,
            icon = Yume.Edit,
            enabled = availability.rowsEnabled,
            onClick = {
                onDismiss()
                onAction(ProfileAction.Edit)
            },
        )
        if (canOverride) {
            GlassMenuItem(
                text = YumeTxt.ProfilesPage.SettingsDialog.SectionOverride,
                icon = Lucide.FileBracesCorner,
                enabled = availability.rowsEnabled,
                onClick = {
                    onDismiss()
                    onAction(ProfileAction.OverrideSettings)
                },
            )
        }
        if (availability.canUpdate) {
            GlassMenuItem(
                text = strings.Update,
                icon = Yume.CircleFadingArrowUp,
                enabled = availability.rowsEnabled,
                onClick = {
                    onDismiss()
                    onAction(ProfileAction.Update)
                },
            )
        }
        GlassPopupSubmenuItem(
            text = strings.Share,
            icon = Yume.Share,
            anchor = anchor,
            open = share.open,
            enabled = availability.rowsEnabled,
            onOpen = share::openAt,
        )
        GlassMenuItem(
            text = strings.Delete,
            icon = Yume.Delete,
            enabled = availability.rowsEnabled,
            contentColor = AppTheme.colors.status.destructive,
            onClick = {
                onDismiss()
                onAction(ProfileAction.Delete)
            },
        )
    }
}
