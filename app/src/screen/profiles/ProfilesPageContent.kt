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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowDownUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgePlus
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProfilesPageContent(
    mainInnerPadding: PaddingValues,
    profiles: List<Profile>,
    state: ProfilesDialogState,
    profilesViewModel: ProfilesViewModel,
    homeViewModel: HomeViewModel,
    isRunning: Boolean,
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()
    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val isBusy = downloadProgress != null
    val addProfile = { state.addRequest = AddProfileRequest() }
    // List actions are ignored while an import or update is in flight. Read live at click time, not
    // captured, so a click that lands after the download finished still goes through.
    val ifIdle: (() -> Unit) -> Unit = { action ->
        if (profilesViewModel.downloadProgress.value == null) action()
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.ProfilesPage.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    // 节点页同款玻璃 chrome:排序(弹拖拽排序 sheet)+ 添加配置 收进一枚胶囊。
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.BadgePlus,
                                    contentDescription = YumeTxt.ProfilesPage.Action.AddProfile,
                                    onClick = addProfile,
                                ),
                                GlassTopAction(
                                    icon = Yume.ArrowDownUp,
                                    contentDescription = YumeTxt.ProfilesPage.Action.Sort,
                                    onClick = { state.showSortSheet = true },
                                ),
                            ),
                        backdrop = LocalTopBarBackdrop.current,
                    )
                },
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            if (profiles.isEmpty()) {
                ProfileEmptyAddGuide(onClick = addProfile)
            } else {
                ProfilesList(
                    profiles = profiles,
                    mainInnerPadding = mainInnerPadding,
                    innerPadding = innerPadding,
                    scrollBehavior = scrollBehavior,
                    isDownloading = isBusy,
                    sortCommitCount = state.sortCommitCount,
                    onReorderProfiles = profilesViewModel::reorderProfiles,
                    onAction = { profile, action ->
                        ifIdle {
                            when (action) {
                                // Editing goes straight to the full 编辑配置 page; the old options dialog is gone.
                                ProfileAction.Edit ->
                                    navigator.push(Route.ProfileEdit(profile.uuid.toString()))

                                ProfileAction.OverrideSettings ->
                                    navigator.push(Route.ProfileOverride(profile.uuid.toString()))

                                // The view model owns the in-flight window: it raises download
                                // progress before its first IPC and clears it when the import ends.
                                ProfileAction.Update -> profilesViewModel.updateProfile(profile.uuid)

                                ProfileAction.ShareFile -> shareProfileFile(profile)

                                ProfileAction.ShareLink -> shareProfileLink(profile)

                                ProfileAction.Delete -> {
                                    state.deleteTarget = profile
                                    state.showDelete = true
                                }
                            }
                        }
                    },
                    onToggleProfile = { profile, active ->
                        ifIdle {
                            scope.launch {
                                if (!active && profile.active && isRunning) {
                                    homeViewModel.stopProxy()
                                }
                                profilesViewModel.setProfileActive(profile.uuid, active)
                            }
                        }
                    },
                )
            }
            // Sheet composition is hosted here; dual-pane renders overlays in the left-pane root
            // Scaffold.
            ProfilesDialogHost(
                state = state,
                profiles = profiles,
                profilesViewModel = profilesViewModel,
            )
        }
    }
}

@Composable
private fun ProfilesList(
    profiles: List<Profile>,
    mainInnerPadding: PaddingValues,
    innerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    isDownloading: Boolean,
    sortCommitCount: Int,
    onReorderProfiles: (Int, Int) -> Unit,
    onAction: (Profile, ProfileAction) -> Unit,
    onToggleProfile: (Profile, Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            onReorderProfiles(from.index, to.index)
        }
    // Applying the sort changes the whole order, and a keyed LazyColumn keeps the previously
    // first visible item in place, which reads as "it scrolled to the bottom". Animate back to the
    // top after the commit (not a jump) so the view returns to what the sheet covered.
    LaunchedEffect(sortCommitCount) {
        if (sortCommitCount > 0) listState.animateScrollToItem(0)
    }
    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        topPadding = UiDp.dp20,
    ) {
        items(items = profiles, key = { it.uuid.toString() }) { profile ->
            ReorderableItem(reorderState, key = profile.uuid.toString()) { _ ->
                ProfileCard(
                    profile = profile,
                    isDownloading = isDownloading,
                    modifier = Modifier.longPressDraggableHandle(),
                    canOverride = true,
                    onAction = onAction,
                    onToggleEnabled = onToggleProfile,
                )
            }
        }
    }
}

@Composable
private fun ProfileEmptyAddGuide(onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    val primary = MiuixTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyResourceIllustration()
        Spacer(modifier = Modifier.height(spacing.space16))
        AppCard(
            modifier = Modifier.wrapContentWidth(),
            cornerRadius = AppTheme.radii.full,
            insideMargin = PaddingValues(horizontal = spacing.space16, vertical = spacing.space10),
            colors =
                CardDefaults.defaultColors(
                    color = primary.copy(alpha = AppTheme.opacity.subtle),
                    contentColor = primary,
                ),
            onClick = onClick,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Yume.BadgePlus,
                    contentDescription = YumeTxt.ProfilesPage.Action.AddProfile,
                    tint = primary,
                    modifier = Modifier.size(UiDp.dp20),
                )
                Spacer(modifier = Modifier.width(spacing.space8))
                Text(
                    text = YumeTxt.ProfilesPage.Action.AddProfile,
                    style = MiuixTheme.textStyles.body1,
                    color = primary,
                )
            }
        }
    }
}
