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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.github.yumeyucca.yumebox.App
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.ShellIcons
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
internal fun ProfilesPageHost(
    mainInnerPadding: PaddingValues,
    profiles: List<Profile>,
    state: ProfilesDialogState,
    profilesViewModel: ProfilesViewModel,
    homeViewModel: HomeViewModel,
    isRunning: Boolean,
    @Suppress("UNUSED_PARAMETER") windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
    sheetHost: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    ProfilesPageContent(
        mainInnerPadding = mainInnerPadding,
        profiles = profiles,
        isDownloading = state.isDownloading,
        onAddProfile = {
            state.profileToEdit = null
            state.showAdd.value = true
        },
        onReorderProfiles = { from, to -> profilesViewModel.reorderProfiles(from, to) },
        onShareProfile = { profile ->
            if (!state.isDownloading) {
                state.shareTarget = profile
                state.showShare.value = true
            }
        },
        onUpdateProfile = { profile ->
            if (!state.isDownloading) {
                state.isDownloading = true
                scope.launch {
                    profilesViewModel.updateProfile(profile.uuid)
                    state.isDownloading = false
                }
            }
        },
        onDeleteProfile = { profile ->
            if (!state.isDownloading) {
                state.deleteTarget = profile
                state.showDelete = true
            }
        },
        onEditProfile = { profile ->
            if (!state.isDownloading) {
                state.editOptionsTarget = profile
                state.showEditOptions = true
            }
        },
        onToggleProfile = { profile ->
            if (!state.isDownloading) {
                scope.launch {
                    if (profile.active && isRunning) {
                        homeViewModel.stopProxy()
                    }
                    profilesViewModel.toggleProfileEnabled(profile.uuid)
                }
            }
        },
        sheetHost = sheetHost,
    )
}

@Composable
internal fun ProfilesPageContent(
    mainInnerPadding: PaddingValues,
    profiles: List<Profile>,
    isDownloading: Boolean,
    onAddProfile: () -> Unit,
    onReorderProfiles: (Int, Int) -> Unit,
    onShareProfile: (Profile) -> Unit,
    onUpdateProfile: (Profile) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onEditProfile: (Profile) -> Unit,
    onToggleProfile: (Profile) -> Unit,
    sheetHost: @Composable () -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.ProfilesPage.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            if (profiles.isEmpty()) {
                ProfileEmptyAddGuide(onClick = onAddProfile)
            } else {
                ProfilesList(
                    profiles = profiles,
                    mainInnerPadding = mainInnerPadding,
                    innerPadding = innerPadding,
                    scrollBehavior = scrollBehavior,
                    isDownloading = isDownloading,
                    onAddProfile = onAddProfile,
                    onReorderProfiles = onReorderProfiles,
                    onShareProfile = onShareProfile,
                    onUpdateProfile = onUpdateProfile,
                    onDeleteProfile = onDeleteProfile,
                    onEditProfile = onEditProfile,
                    onToggleProfile = onToggleProfile,
                )
            }
            // Sheet composition is hosted here; dual-pane renders overlays in the left-pane root
            // Scaffold.
            sheetHost()
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
    onAddProfile: () -> Unit,
    onReorderProfiles: (Int, Int) -> Unit,
    onShareProfile: (Profile) -> Unit,
    onUpdateProfile: (Profile) -> Unit,
    onDeleteProfile: (Profile) -> Unit,
    onEditProfile: (Profile) -> Unit,
    onToggleProfile: (Profile) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            // The add card occupies the first LazyColumn slot but is not a profile. Keep it
            // outside the sortable range before forwarding indices to the profiles list.
            val fromProfile = from.index - 1
            val toProfile = to.index - 1
            if (fromProfile < 0 || toProfile < 0) {
                return@rememberReorderableLazyListState
            }
            onReorderProfiles(fromProfile, toProfile)
        }
    val importedDir = App.instance.filesDir.resolve("imported")

    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        topPadding = UiDp.dp20,
    ) {
        item(key = "profile_add") {
            ProfileAddCard(onClick = onAddProfile)
        }
        items(items = profiles, key = { it.uuid.toString() }) { profile ->
            ReorderableItem(reorderState, key = profile.uuid.toString()) { isDragging ->
                ProfileCard(
                    profile = profile,
                    workDir = importedDir,
                    isDownloading = isDownloading,
                    modifier =
                        Modifier
                            .longPressDraggableHandle()
                            .alpha(if (isDragging) 0.9f else 1f),
                    onExport = { onShareProfile(it) },
                    onUpdate = { onUpdateProfile(it) },
                    onDelete = { onDeleteProfile(it) },
                    onEdit = { onEditProfile(it) },
                    onToggleEnabled = { onToggleProfile(it) },
                )
            }
        }
    }
}

@Composable
private fun ProfileAddCard(onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    val primary = MiuixTheme.colorScheme.primary

    AppCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = spacing.space12),
        insideMargin = PaddingValues(spacing.space16),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ShellIcons.AddProfile,
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
            cornerRadius = 100,
            insideMargin = PaddingValues(horizontal = spacing.space16, vertical = spacing.space10),
            colors =
                top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                    color = primary.copy(alpha = 0.10f),
                    contentColor = primary,
                ),
            onClick = onClick,
            pressFeedbackType = PressFeedbackType.Sink,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ShellIcons.AddProfile,
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
