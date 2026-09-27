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

package com.github.yumeyucca.yumebox.screen.profiles


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumeyucca.yumebox.data.store.LinkOpenMode
import com.github.yumeyucca.yumebox.data.store.ProfileLink
import com.github.yumeyucca.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumeyucca.yumebox.presentation.component.SectionCard
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LinkSettingsContent(
    links: List<ProfileLink>,
    linkOpenMode: LinkOpenMode,
    defaultLinkId: String,
    onOpenModeChange: (LinkOpenMode) -> Unit,
    onDefaultLinkChange: (String) -> Unit,
    onAddLink: () -> Unit,
    onDeleteLink: (String) -> Unit,
    onOpenLink: (ProfileLink) -> Unit,
    onClose: () -> Unit,
) {
    val spacing = AppTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.space16),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
    ) {
        LinkOpenModeSection(linkOpenMode, onOpenModeChange)
        DefaultLinkSection(links, defaultLinkId, onDefaultLinkChange)
        ProfileLinkList(links, onDeleteLink, onOpenLink)
        LinkSettingsActions(onClose, onAddLink)
    }
}

@Composable
private fun LinkOpenModeSection(mode: LinkOpenMode, onChange: (LinkOpenMode) -> Unit) {
    SectionCard(title = YumeTxt.ProfilesPage.LinkSettings.OpenMode) {
        PreferenceEnumItem(
            title = YumeTxt.ProfilesPage.LinkSettings.OpenMode,
            currentValue = mode,
            items =
                listOf(
                    YumeTxt.ProfilesPage.LinkSettings.OpenModeInApp,
                    YumeTxt.ProfilesPage.LinkSettings.OpenModeExternal,
                ),
            values = listOf(LinkOpenMode.IN_APP, LinkOpenMode.EXTERNAL_BROWSER),
            onValueChange = onChange,
        )
    }
}

@Composable
private fun DefaultLinkSection(
    links: List<ProfileLink>,
    defaultLinkId: String,
    onChange: (String) -> Unit,
) {
    if (links.isEmpty()) return

    SectionCard(title = YumeTxt.ProfilesPage.LinkSettings.DefaultLink) {
        PreferenceEnumItem(
            title = YumeTxt.ProfilesPage.LinkSettings.DefaultLink,
            summary = YumeTxt.ProfilesPage.LinkSettings.DefaultLinkSummary,
            currentValue = links.firstOrNull { it.id == defaultLinkId }?.id ?: links.first().id,
            items = links.map { it.name },
            values = links.map { it.id },
            onValueChange = onChange,
        )
    }
}

@Composable
private fun ProfileLinkList(
    links: List<ProfileLink>,
    onDelete: (String) -> Unit,
    onOpen: (ProfileLink) -> Unit,
) {
    if (links.isEmpty()) return

    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val sizes = AppTheme.sizes
    SectionCard(title = YumeTxt.ProfilesPage.LinkSettings.Title) {
        Column(modifier = Modifier.fillMaxWidth()) {
            links.forEachIndexed { index, link ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(link) }
                            .padding(horizontal = spacing.space16, vertical = spacing.space12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = link.name, style = MiuixTheme.textStyles.body1)
                        Text(
                            text = link.url,
                            style = MiuixTheme.textStyles.body2,
                            color =
                                MiuixTheme.colorScheme.onSurface.copy(
                                    alpha = opacity.secondaryText
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDelete(link.id) }) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = YumeTxt.Component.ProfileCard.Delete,
                            tint = MiuixTheme.colorScheme.error,
                        )
                    }
                }
                if (index < links.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = spacing.space16),
                        thickness = sizes.thinDividerThickness,
                        color = MiuixTheme.colorScheme.outline.copy(alpha = opacity.outline),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkSettingsActions(onClose: () -> Unit, onAdd: () -> Unit) {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space12),
    ) {
        TextButton(
            text = YumeTxt.ProfilesPage.LinkSettings.Close,
            onClick = onClose,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onAdd,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(
                YumeTxt.ProfilesPage.LinkSettings.AddLink,
                color = MiuixTheme.colorScheme.onPrimary,
            )
        }
    }
}
