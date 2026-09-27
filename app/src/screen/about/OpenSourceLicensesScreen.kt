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

package com.github.yumeyucca.yumebox.screen.about


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumeyucca.yumebox.R
import com.github.yumeyucca.yumebox.common.util.openUrl
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun OpenSourceLicensesScreen(navigator: Navigator) {
    val context = LocalContext.current
    val spacing = AppTheme.spacing

    val scrollBehavior = MiuixScrollBehavior()

    BackHandler { navigator.pop() }

    val libraries by produceLibraries(R.raw.aboutlibraries)
    val libraryItems = remember(libraries) { libraries?.libraries.orEmpty() }

    Scaffold(
        topBar = {
            TopBar(title = YumeTxt.OpenSourceLicenses.Title, scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            // Match About page: breathing room under the large TopBar title.
            item { Spacer(modifier = Modifier.height(UiDp.dp24)) }

            if (libraryItems.isNotEmpty()) {
                items(
                    items = libraryItems,
                    key = { library ->
                        "${library.uniqueId}:${library.artifactId}:${library.name}"
                    },
                ) { library ->
                    LibraryItem(
                        library = library,
                        onClick = {
                            val url = library.projectUrl
                            if (!url.isNullOrBlank()) {
                                openUrl(context, url)
                            }
                        },
                    )
                }

                item { Spacer(modifier = Modifier.height(spacing.space24)) }
            }
        }
    }
}

@Composable
private fun LibraryItem(library: Library, onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    val hasUrl = !library.projectUrl.isNullOrBlank()

    AppCard(
        modifier = Modifier.padding(bottom = spacing.space12),
        insideMargin = PaddingValues(spacing.space0),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasUrl) {
                            Modifier.clickable(onClick = onClick)
                        } else {
                            Modifier
                        }
                    )
                    .padding(spacing.space16),
            verticalArrangement = Arrangement.spacedBy(spacing.space10),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = library.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                library.artifactVersion?.let { version ->
                    Text(
                        text = version,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = spacing.space12),
                    )
                }
            }

            library.developers.firstOrNull()?.name?.let { author ->
                Text(
                    text = author,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (library.licenses.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space6),
                    verticalArrangement = Arrangement.spacedBy(spacing.space6),
                ) {
                    library.licenses.forEach { license -> LicenseChip(licenseName = license.name) }
                }
            }
        }
    }
}

@Composable
private fun LicenseChip(licenseName: String) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity

    Box(
        modifier =
            Modifier
                .squircleSurface(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle), radii.radius12)
                .padding(horizontal = spacing.space10, vertical = spacing.space4)
    ) {
        Text(
            text = licenseName,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

/** Prefer website, then SCM url — both come from the artifact pom. */
private val Library.projectUrl: String?
    get() = website?.takeIf { it.isNotBlank() } ?: scm?.url?.takeIf { it.isNotBlank() }
