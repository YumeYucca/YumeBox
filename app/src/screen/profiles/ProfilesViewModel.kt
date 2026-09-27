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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.screen.profiles


import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumeyucca.yumebox.data.store.LinkOpenMode
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.data.store.ProfileLink
import com.github.yumeyucca.yumebox.data.store.ProfileLinksStore
import com.github.yumeyucca.yumebox.runtime.api.FetchObserver
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.ProfileUpdateReport
import com.github.yumeyucca.yumebox.runtime.client.ProfilePatch
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import timber.log.Timber
import java.util.*

class ProfilesViewModel(
    application: Application,
    private val profilesRepository: ProfilesRepository,
    profileLinksStorage: ProfileLinksStore,
) :
    AndroidContractStateViewModel<ProfilesUiState, ProfilesUiEffect>(
        application,
        ProfilesUiState(),
    ) {
    val linkOpenMode: Preference<LinkOpenMode> = profileLinksStorage.linkOpenMode
    val links: Preference<List<ProfileLink>> = profileLinksStorage.links
    val defaultLinkId: Preference<String> = profileLinksStorage.defaultLinkId

    fun setOpenMode(mode: LinkOpenMode) = linkOpenMode.set(mode)

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    init {
        refreshProfiles()
    }

    // Fault barrier: any refresh failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun refreshProfiles() {
        viewModelScope.launch {
            try {
                setLoading(true)
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()

                _profiles.value = allProfiles
                _activeProfile.value = active
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh profiles")
                showError(
                    YumeTxt.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown")
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any create failure rolls back the staged profile and becomes UI error
    // state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        interval: Long = 0L,
        fileUri: Uri? = null,
        ageSecretKey: String = "",
    ) {
        viewModelScope.launch {
            var createdUuid: UUID? = null
            try {
                setLoading(true)
                // File imports must persist the content/file URI as source so ProfileProcessor can
                // re-materialize config.yaml and prefetch proxy/rule providers on update.
                val resolvedSource =
                    source.ifBlank {
                        if (type == Profile.Type.File) fileUri?.toString().orEmpty() else ""
                    }
                val uuid =
                    profilesRepository.createProfile(type, name, resolvedSource, ageSecretKey)
                createdUuid = uuid

                _downloadProgress.value =
                    DownloadProgress(percent = 0, message = YumeTxt.ProfilesVM.Progress.Preparing)

                val observer = FetchObserver { status ->
                    _downloadProgress.value = status.toDownloadProgress()
                }

                if (type == Profile.Type.File && fileUri != null) {
                    getApplication<Application>().copyProfileImport(fileUri, uuid)
                }

                val report = profilesRepository.updateProfile(uuid, observer)
                _downloadProgress.value =
                    DownloadProgress(
                        percent = 100,
                        message = YumeTxt.ProfilesVM.Progress.ImportComplete,
                        isCompleted = true,
                    )

                showMessage(successMessage(report, displayName = name, isCreate = true))
                refreshProfiles()
                Timber.i("Profile created: $uuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to create profile")
                createdUuid?.let { uuid ->
                    runCatching { profilesRepository.deleteProfile(uuid) }
                        .onFailure { deleteError ->
                            Timber.w(deleteError, "Failed to rollback profile creation: $uuid")
                        }
                }
                refreshProfiles()
                showError(YumeTxt.ProfilesVM.Message.AddFailed.format(error.message ?: "Unknown"))
                _downloadProgress.value = null
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any clone failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun cloneProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val newUuid = profilesRepository.cloneProfile(uuid)
                showMessage(YumeTxt.ProfilesVM.Message.ProfileAdded.format("Clone"))
                refreshProfiles()
                Timber.i("Profile cloned: from=$uuid to=$newUuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to clone profile")
                showError(YumeTxt.ProfilesVM.Message.AddFailed.format(error.message ?: "Unknown"))
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any delete failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun deleteProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.deleteProfile(uuid)
                showMessage(YumeTxt.ProfilesVM.Message.ProfileDeleted)
                refreshProfiles()
                Timber.i("Profile deleted: $uuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to delete profile")
                showError(
                    YumeTxt.ProfilesVM.Message.DeleteFailed.format(error.message ?: "Unknown")
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any activate failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun activateProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.setActiveProfile(uuid)
                showMessage(YumeTxt.ProfilesVM.Message.ProfileUpdated.format("Active"))
                refreshProfiles()
                Timber.i("Profile activated: $uuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to activate profile")
                showError(
                    YumeTxt.ProfilesVM.Message.ToggleFailed.format(error.message ?: "Unknown")
                )
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any update/fetch failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun updateProfile(uuid: UUID) {
        viewModelScope.launch {
            try {
                setLoading(true)
                _downloadProgress.value =
                    DownloadProgress(percent = 0, message = YumeTxt.ProfilesVM.Progress.Preparing)

                val observer = FetchObserver { status ->
                    _downloadProgress.value = status.toDownloadProgress()
                }

                val report = profilesRepository.updateProfile(uuid, observer)

                _downloadProgress.value =
                    DownloadProgress(
                        percent = 100,
                        message = YumeTxt.ProfilesVM.Progress.ImportComplete,
                        isCompleted = true,
                    )
                val displayName =
                    profilesRepository.queryProfileByUUID(uuid)?.name ?: uuid.toString()
                showMessage(successMessage(report, displayName = displayName, isCreate = false))
                refreshProfiles()
                Timber.i("Profile updated: $uuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to update profile")
                showError(
                    YumeTxt.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown")
                )
                _downloadProgress.value = null
            } finally {
                setLoading(false)
            }
        }
    }

    // Fault barrier: any patch failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun patchProfile(uuid: UUID, patch: ProfilePatch) {
        viewModelScope.launch {
            try {
                setLoading(true)
                profilesRepository.patchProfile(uuid, patch)
                showMessage(YumeTxt.ProfilesVM.Message.ProfileUpdated.format(patch.name))
                refreshProfiles()
                Timber.i("Profile patched: $uuid")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to patch profile")
                showError(
                    YumeTxt.ProfilesVM.Message.UpdateFailed.format(error.message ?: "Unknown")
                )
            } finally {
                setLoading(false)
            }
        }
    }

    fun importProfileFromFile(uri: Uri, name: String) {
        createProfile(type = Profile.Type.File, name = name, fileUri = uri)
    }

    // Fault barrier: any reorder failure logs and re-syncs the list from storage (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun reorderProfiles(from: Int, to: Int) {
        viewModelScope.launch {
            try {
                val current = _profiles.value
                if (from !in current.indices || to !in current.indices || from == to) return@launch

                val reordered = current.toMutableList()
                val moved = reordered.removeAt(from)
                reordered.add(to, moved)

                _profiles.value = reordered
                profilesRepository.reorderProfiles(reordered.map { it.uuid })
                Timber.d("Profiles reordered: $from->$to")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to reorder profiles")
                refreshProfiles()
            }
        }
    }

    // Fault barrier: any toggle failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    fun toggleProfileEnabled(uuid: UUID) {
        viewModelScope.launch {
            try {
                val profile =
                    profilesRepository.queryProfileByUUID(uuid) ?: error("Profile not found: $uuid")

                if (profile.active) {
                    profilesRepository.clearActiveProfile(profile)
                    showMessage(YumeTxt.ProfilesVM.Message.ProfileUpdated.format(profile.name))
                } else {
                    profilesRepository.setActiveProfile(uuid)
                    showMessage(YumeTxt.ProfilesVM.Message.ProfileUpdated.format(profile.name))
                }
                refreshProfiles()
                Timber.d("Profile toggled: $uuid, active=${!profile.active}")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to toggle profile")
                showError(
                    YumeTxt.ProfilesVM.Message.ToggleFailed.format(error.message ?: "Unknown")
                )
            }
        }
    }

    fun clearDownloadProgress() {
        _downloadProgress.value = null
    }

    fun clearError() {
        clearErrorState()
    }

    fun clearMessage() {
        clearMessageState()
    }

    /**
     * Soft warnings for partial provider prefetch. Never routes through showError so the Add sheet
     * stays on the success path when the main config committed.
     */
    private fun successMessage(
        report: ProfileUpdateReport,
        displayName: String,
        isCreate: Boolean,
    ): String {
        val providers = report.providers
        if (providers.failedNames.isNotEmpty()) {
            val preview =
                providers.failedNames.take(3).joinToString(", ").let { names ->
                    if (providers.failedNames.size > 3) "$names…" else names
                }
            return YumeTxt.ProfilesVM.Message.ProvidersPartial.format(
                providers.failedNames.size,
                preview,
            )
        }
        if (providers.discoveryAnomaly) {
            return YumeTxt.ProfilesVM.Message.ProvidersUndiscovered
        }
        return if (isCreate) {
            YumeTxt.ProfilesVM.Message.ProfileAdded.format(displayName)
        } else {
            YumeTxt.ProfilesVM.Message.ProfileUpdated.format(displayName)
        }
    }

    private fun showError(message: String) {
        postError(message, ProfilesUiEffect.ShowError(message))
    }

    private fun showMessage(message: String) {
        postMessage(message, ProfilesUiEffect.ShowMessage(message))
    }
}
