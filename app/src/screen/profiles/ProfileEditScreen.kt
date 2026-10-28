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


import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.App
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.icon.yume.Eye
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.util.OverrideEditorStore
import com.github.yumeyucca.yumebox.presentation.util.readDisplaySize
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilePatch
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standalone "编辑配置" page: profile fields and config-file facts only.
 *
 * Override selection moved to [ProfileOverrideScreen]; this page keeps the same label/value input
 * rows as the add-subscription sheet, plus the config file's update time and size.
 */
@Composable
fun ProfileEditScreen(navigator: Navigator, profileId: String) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val pageListState = rememberLazyListState()

    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val profile = remember(profiles, profileId) {
        profiles.firstOrNull { it.uuid.toString() == profileId }
    }

    var editName by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var editSource by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var editAgeSecretKey by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var ageSecretKeyEdited by remember(profileId) { mutableStateOf(false) }
    var ageSecretVisible by remember(profileId) { mutableStateOf(false) }
    var isSaving by remember(profileId) { mutableStateOf(false) }
    var replacementFileUri by remember(profileId) { mutableStateOf<Uri?>(null) }
    var replacementFileSize by remember(profileId) { mutableStateOf(0L) }

    val fileLaunchers =
        rememberProfileImportLaunchers(
            context = context,
            onFileSelected = { uri, _ ->
                replacementFileUri = uri
                replacementFileSize = readDisplaySize(context, uri)
            },
            onUnsupportedFile = {
                context.toast(YumeTxt.ProfilesPage.Validation.YamlOnly)
            },
            onQrDecoded = {},
        )

    // Seed the editable fields once the profile arrives; keyed on the id only so a list refresh
    // emitting an equal profile cannot wipe edits in progress.
    LaunchedEffect(profile?.uuid) {
        val target = profile ?: return@LaunchedEffect
        editName = TextFieldValue(target.name, TextRange(target.name.length))
        if (target.type == Profile.Type.Url) {
            editSource = TextFieldValue(target.source, TextRange(target.source.length))
        }
    }

    var configFileSize by remember(profile?.uuid, profile?.updatedAt) {
        mutableStateOf<Long?>(null)
    }
    LaunchedEffect(profile?.uuid, profile?.updatedAt) {
        val target = profile
        configFileSize =
            if (target == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    runCatching {
                        importedConfigFile(target).takeIf { it.exists() }?.length()
                    }.getOrNull()
                }
            }
    }
    val updatedAtText = remember(profile?.updatedAt) {
        formatUpdatedAt(profile?.updatedAt ?: -1L)
    }
    val configFileSizeText = remember(configFileSize) {
        configFileSize?.let(::formatBytes) ?: "—"
    }
    val fileRowSizeText = remember(replacementFileUri, replacementFileSize, configFileSize) {
        val size =
            if (replacementFileUri != null) replacementFileSize else configFileSize ?: 0L
        if (size > 0L) formatBytes(size) else ""
    }

    val openConfigInEditor = {
        val target = profile
        if (target != null) {
            val configFile = importedConfigFile(target)
            openProfileConfigPreview(
                targetFile = configFile,
                missingMessage =
                    YumeTxt.ProfilesPage.SettingsDialog.ConfigMissing.format(
                        configFile.absolutePath
                    ),
                editable = true,
                onReadFailed = context::toast,
            ) { content, callback ->
                OverrideEditorStore.setupConfigPreview(
                    title = target.name,
                    content = content,
                    language = LanguageScope.Yaml,
                    callback = callback,
                )
                navigator.push(Route.OverrideConfigPreview)
            }
        }
    }

    val saveSettings = {
        if (!isSaving) {
            scope.launch {
                val target = profile
                if (target == null) {
                    navigator.pop()
                    return@launch
                }
                isSaving = true
                runCatching {
                    val trimmedName = editName.text.trim()
                    val trimmedSource = editSource.text.trim()
                    val trimmedAgeSecretKey = editAgeSecretKey.text.trim()
                    val replacementUri = replacementFileUri
                    val targetSource =
                        when {
                            target.type == Profile.Type.Url && trimmedSource.isNotEmpty() ->
                                trimmedSource
                            target.type == Profile.Type.File && replacementUri != null ->
                                replacementUri.toString()
                            else -> target.source
                        }
                    val hasMetaChanges =
                        trimmedName != target.name ||
                                targetSource != target.source ||
                                ageSecretKeyEdited
                    if (trimmedName.isNotEmpty() && targetSource.isNotEmpty() && hasMetaChanges) {
                        if (target.type == Profile.Type.File && replacementUri != null) {
                            App.instance.copyProfileImport(replacementUri, target.uuid)
                        }
                        profilesViewModel.patchProfile(
                            uuid = target.uuid,
                            patch =
                                ProfilePatch(
                                    name = trimmedName,
                                    source = targetSource,
                                    interval = target.interval,
                                    updateAgeSecretKey = ageSecretKeyEdited,
                                    ageSecretKey =
                                        if (ageSecretKeyEdited) trimmedAgeSecretKey else null,
                                ),
                        )
                    }
                }
                    .onSuccess { navigator.pop() }
                    .onFailure { error ->
                        context.toast(error.message ?: YumeTxt.Util.Error.UnknownError)
                    }
                isSaving = false
            }
        }
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

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.ProfilesPage.SettingsDialog.EditProfile,
                scrollBehavior = scrollBehavior,
                actions = {
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.Edit,
                                    contentDescription =
                                        YumeTxt.ProfilesPage.SettingsDialog.OpenConfig,
                                    onClick = openConfigInEditor,
                                ),
                                GlassTopAction(
                                    icon = Yume.Check,
                                    contentDescription = YumeTxt.ProfilesPage.Button.Confirm,
                                    onClick = saveSettings,
                                ),
                            ),
                        backdrop = LocalTopBarBackdrop.current,
                    )
                },
            )
        }
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = pageListState,
        ) {
            item(key = "profile-edit-status") {
                Title(YumeTxt.ProfilesPage.SettingsDialog.ProfileStatus)
                AppCard {
                    SwitchPreference(
                        title = YumeTxt.ProfilesPage.SettingsDialog.ProfileEnabled,
                        checked = profile?.active == true,
                        enabled = profile != null,
                        onCheckedChange = { active -> setProfileActive(active) },
                    )
                }
            }

            item(key = "profile-edit-info") {
                Title(YumeTxt.ProfilesPage.SettingsDialog.ProfileInfo)
                AppCard {
                    Column {
                        AppTextFieldRow(
                            label = YumeTxt.ProfilesPage.Input.ProfileName,
                            value = editName,
                            onValueChange = { editName = it },
                        )

                        when (profile?.type) {
                            Profile.Type.Url ->
                                AppTextFieldRow(
                                    label = YumeTxt.ProfilesPage.Input.SubscriptionUrl,
                                    value = editSource,
                                    onValueChange = { editSource = it },
                                    keyboardOptions =
                                        KeyboardOptions(keyboardType = KeyboardType.Uri),
                                )
                            Profile.Type.File ->
                                ArrowPreference(
                                    title = YumeTxt.ProfilesPage.Input.SelectFile,
                                    endActions = {
                                        if (fileRowSizeText.isNotEmpty()) {
                                            Text(
                                                text = fileRowSizeText,
                                                style = MiuixTheme.textStyles.body2,
                                                color =
                                                    MiuixTheme.colorScheme
                                                        .onSurfaceVariantSummary,
                                                maxLines = 1,
                                            )
                                        }
                                    },
                                    onClick = fileLaunchers.pickFile,
                                )
                            else -> Unit
                        }
                    }
                }
            }

            item(key = "profile-edit-age") {
                Title(YumeTxt.ProfilesPage.SettingsDialog.AgeSection)
                AppCard {
                    Column {
                        AppTextFieldRow(
                            label = YumeTxt.ProfilesPage.Input.AgeSecretKey,
                            value = editAgeSecretKey,
                            onValueChange = {
                                editAgeSecretKey = it
                                ageSecretKeyEdited = true
                            },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        if (ageSecretVisible) {
                                            KeyboardType.Text
                                        } else {
                                            KeyboardType.Password
                                        },
                                ),
                            trailingIcon = {
                                AppTextFieldRowIcon(
                                    icon = Yume.Eye,
                                    contentDescription =
                                        YumeTxt.ProfilesPage.Input.AgeSecretKey,
                                    tint =
                                        if (ageSecretVisible) {
                                            MiuixTheme.colorScheme.primary
                                        } else {
                                            Color.Unspecified
                                        },
                                    onClick = { ageSecretVisible = !ageSecretVisible },
                                )
                            },
                        )
                    }
                }
                SectionSummary(YumeTxt.ProfilesPage.SettingsDialog.AgeSecretKeySummary)
            }

            item(key = "profile-edit-file") {
                Title(YumeTxt.ProfilesPage.SettingsDialog.ConfigFile)
                AppCard {
                    Column {
                        BasicComponent(
                            title = YumeTxt.ProfilesPage.SettingsDialog.UpdatedAt,
                            endActions = {
                                Text(
                                    text = updatedAtText,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                        )
                        BasicComponent(
                            title = YumeTxt.ProfilesPage.SettingsDialog.ConfigFileSize,
                            endActions = {
                                Text(
                                    text = configFileSizeText,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun formatUpdatedAt(timestamp: Long): String =
    if (timestamp <= 0L) {
        "—"
    } else {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
