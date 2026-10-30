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


import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.App
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.common.util.formatDateTime
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.navigation.LocalConfigPreviewSlot
import com.github.yumeyucca.yumebox.presentation.navigation.ConfigPreviewRequest
import com.github.yumeyucca.yumebox.presentation.navigation.ConfigPreviewSlot
import com.github.yumeyucca.yumebox.presentation.util.readDisplaySize
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Standalone "编辑配置" page: profile fields and config-file facts only.
 *
 * Override selection moved to [ProfileOverrideScreen]; this page keeps the same label/value input
 * rows as the add-subscription sheet, plus the config file's update time and size.
 *
 * The page is a renderer: [rememberProfileEditDraft] owns the edit-in-progress copy and the loads
 * that fill it, so nothing here writes an editable field directly.
 */
@Composable
fun ProfileEditScreen(navigator: Navigator, profileId: String) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val context = LocalContext.current
    val configPreviewSlot = LocalConfigPreviewSlot.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val pageListState = rememberLazyListState()

    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val profile = remember(profiles, profileId) {
        profiles.firstOrNull { it.uuid.toString() == profileId }
    }

    val draft = rememberProfileEditDraft(profileId = profileId, profile = profile)
    val pickConfigFile = rememberProfileEditFilePicker(context = context, draft = draft)

    val updatedAtText = remember(profile?.updatedAt) {
        formatUpdatedAt(profile?.updatedAt ?: -1L)
    }

    val setProfileActive = { active: Boolean ->
        val target = profile
        if (target != null) {
            scope.launch {
                if (!active && target.active && isRunning) {
                    homeViewModel.stopProxy()
                }
                profilesViewModel.setProfileActive(target.uuid, active)
            }
        }
    }
    val openConfigInEditor: () -> Unit = {
        openProfileConfigInEditor(profile, context, scope, configPreviewSlot, navigator)
    }
    val saveSettings: () -> Unit = {
        saveProfileEdits(draft, profile, scope, context, navigator, profilesViewModel)
    }

    ProfileSettingsPage(
        title = YumeTxt.ProfilesPage.SettingsDialog.EditProfile,
        scrollBehavior = scrollBehavior,
        listState = pageListState,
        actions = { ProfileEditTopActions(openConfigInEditor, saveSettings) },
    ) {
        item(key = "profile-edit-status") { ProfileStatusSection(profile, setProfileActive) }
        item(key = "profile-edit-info") {
            ProfileIdentitySection(profile?.type, draft, pickConfigFile)
        }
        item(key = "profile-edit-age") { ProfileAgeSection(draft) }
        item(key = "profile-edit-file") {
            ProfileConfigFileSection(updatedAtText, draft.configFileSizeText)
        }
    }
}

/** The top-bar actions of this page: open the config in the editor, then save the draft. */
@Composable
private fun ProfileEditTopActions(onOpenConfig: () -> Unit, onSave: () -> Unit) {
    GlassTopActionPill(
        actions =
            listOf(
                GlassTopAction(
                    icon = Yume.Edit,
                    contentDescription = YumeTxt.ProfilesPage.SettingsDialog.OpenConfig,
                    onClick = onOpenConfig,
                ),
                GlassTopAction(
                    icon = Yume.Check,
                    contentDescription = YumeTxt.ProfilesPage.Button.Confirm,
                    onClick = onSave,
                ),
            ),
        backdrop = LocalTopBarBackdrop.current,
    )
}

/** The profile's on/off switch; [profile] stays null until the list has the row. */
@Composable
private fun ProfileStatusSection(profile: Profile?, onActiveChange: (Boolean) -> Unit) {
    Title(YumeTxt.ProfilesPage.SettingsDialog.ProfileStatus)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.ProfilesPage.SettingsDialog.ProfileEnabled,
            checked = profile?.active == true,
            enabled = profile != null,
            onCheckedChange = { active -> onActiveChange(active) },
        )
    }
}

/**
 * The name and source rows.
 *
 * [draft] is this page's own state holder, handed over instead of its seven separate fields so the
 * section stays inside the parameter budget; every write still goes through the holder's operations.
 */
@Composable
private fun ProfileIdentitySection(
    sourceType: Profile.Type?,
    draft: ProfileEditDraft,
    onPickFile: () -> Unit,
) {
    Title(YumeTxt.ProfilesPage.SettingsDialog.ProfileInfo)
    AppCard {
        Column {
            ProfileNameField(
                value = draft.name,
                onValueChange = draft::onNameChange,
            )

            ProfileSourceField(
                sourceType = sourceType,
                urlValue = draft.source,
                onUrlChange = draft::onSourceChange,
                fileSizeText = draft.fileRowSizeText,
                onPickFile = onPickFile,
            )
        }
    }
}

/** The age-secret row; [draft] holds the key and whether it is revealed. */
@Composable
private fun ProfileAgeSection(draft: ProfileEditDraft) {
    Title(YumeTxt.ProfilesPage.SettingsDialog.AgeSection)
    AppCard {
        Column {
            ProfileAgeSecretField(
                value = draft.ageSecretKey,
                onValueChange = draft::onAgeSecretKeyChange,
                visible = draft.ageSecretVisible,
                onVisibilityChange = draft::onAgeSecretVisibilityChange,
            )
        }
    }
    SectionSummary(YumeTxt.ProfilesPage.SettingsDialog.AgeSecretKeySummary)
}

/** The config file's read-only facts: when it last updated, and how large it is. */
@Composable
private fun ProfileConfigFileSection(updatedAtText: String, sizeText: String) {
    Title(YumeTxt.ProfilesPage.SettingsDialog.ConfigFile)
    AppCard {
        Column {
            ProfileInfoRow(
                title = YumeTxt.ProfilesPage.SettingsDialog.UpdatedAt,
                value = updatedAtText,
            )
            ProfileInfoRow(
                title = YumeTxt.ProfilesPage.SettingsDialog.ConfigFileSize,
                value = sizeText,
            )
        }
    }
}

/**
 * The config-file picker of this page: a picked YAML becomes [draft]'s replacement source, an
 * unsupported file is toasted instead.
 */
@Composable
private fun rememberProfileEditFilePicker(
    context: Context,
    draft: ProfileEditDraft,
): () -> Unit =
    rememberProfileConfigFilePicker(
        context = context,
        onFileSelected = { uri, _ ->
            draft.setReplacementFile(uri, readDisplaySize(context, uri))
        },
        onUnsupportedFile = {
            context.toast(YumeTxt.ProfilesPage.Validation.YamlOnly)
        },
    )

/** Opens the profile's config file in the editor. */
private fun openProfileConfigInEditor(
    profile: Profile?,
    context: Context,
    scope: CoroutineScope,
    configPreviewSlot: ConfigPreviewSlot,
    navigator: Navigator,
) {
    // The config file lives on disk: read it off the main thread, then hand it to the editor.
    val target = profile
    if (target != null) {
        val configFile = importedConfigFile(target)
        scope.launch {
            val content =
                withContext(Dispatchers.IO) {
                    readProfileConfigFile(
                        targetFile = configFile,
                        missingMessage =
                            YumeTxt.ProfilesPage.SettingsDialog.ConfigMissing.format(
                                configFile.absolutePath
                            ),
                    )
                }
                    .getOrElse { error ->
                        context.toast(error.message.orEmpty())
                        return@launch
                    }
            val previewId =
                configPreviewSlot.put(
                    ConfigPreviewRequest(
                        title = target.name,
                        content = content,
                        language = LanguageScope.Yaml,
                        onSave = { updatedContent ->
                            runCatching { configFile.writeText(updatedContent) }
                                .getOrElse { error ->
                                    throw IllegalStateException(
                                        error.message
                                            ?: YumeTxt.ProfilesPage.SettingsDialog.SaveFailed,
                                        error,
                                    )
                                }
                        },
                    )
                )
            navigator.push(Route.OverrideConfigPreview(previewId))
        }
    }
}

/**
 * Saves [draft]'s patch for [profile] and pops the page.
 *
 * [profilesViewModel] is only the patch sink: the draft still decides what the patch is, and a
 * cancelled save is rethrown instead of being reported as a failure.
 */
private fun saveProfileEdits(
    draft: ProfileEditDraft,
    profile: Profile?,
    scope: CoroutineScope,
    context: Context,
    navigator: Navigator,
    profilesViewModel: ProfilesViewModel,
) {
    if (!draft.isSaving) {
        scope.launch {
            val target = profile
            if (target == null) {
                navigator.pop()
                return@launch
            }
            draft.beginSave()
            runCatching {
                val replacement = draft.replacementFile
                val patch = draft.patchFor(target)
                if (patch != null) {
                    if (target.type == Profile.Type.File && replacement != null) {
                        App.instance.copyProfileImport(replacement.uri, target.uuid)
                    }
                    profilesViewModel.patchProfile(uuid = target.uuid, patch = patch)
                }
            }
                .onSuccess { navigator.pop() }
                .onFailure { error ->
                    // Leaving the screen cancels the save; that is not a save failure.
                    if (error is CancellationException) throw error
                    context.toast(error.message ?: YumeTxt.Util.Error.UnknownError)
                }
            draft.endSave()
        }
    }
}

/** One read-only "label — value" row of the profile's config file card. */
@Composable
private fun ProfileInfoRow(title: String, value: String) {
    BasicComponent(
        title = title,
        endActions = {
            Text(text = value, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
        },
    )
}

private fun formatUpdatedAt(timestamp: Long): String =
    if (timestamp <= 0L) "—" else formatDateTime(timestamp, "yyyy-MM-dd HH:mm")
