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

package com.github.yumeyucca.yumebox.screen.settings


import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumeyucca.yumebox.core.presentation.LoadableState
import com.github.yumeyucca.yumebox.data.controller.AccessControlController
import com.github.yumeyucca.yumebox.data.model.AccessControlImportStrategy
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.service.root.RootPackageShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessControlViewModel(
    application: Application,
    private val settings: NetworkSettingsStore,
    private val controller: AccessControlController,
) :
    AndroidContractStateViewModel<
            AccessControlViewModel.UiState,
            AccessControlViewModel.AccessControlUiEffect,
            >(
        application,
        UiState(
            showSystemApps = settings.accessControlShowSystemApps.value,
            sortMode = settings.accessControlSortMode.value,
            selectedFirst = settings.accessControlSelectedFirst.value,
            importStrategy = settings.accessControlImportStrategy.value,
        ),
    ) {
    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystemApp: Boolean,
        val installTime: Long = 0L,
        val updateTime: Long = 0L,
    )

    data class UiState(
        override val isLoading: Boolean = true,
        val apps: List<AppInfo> = emptyList(),
        val selectedPackages: Set<String> = emptySet(),
        /**
         * Selection snapshot used for ordering the currently visible list. It is refreshed when
         * the screen is entered, so changing a checkbox does not immediately move that row.
         */
        val initialSelectedPackages: Set<String> = emptySet(),
        val searchQuery: String = "",
        val showSystemApps: Boolean = false,
        val sortMode: AccessControlSortMode = AccessControlSortMode.LABEL,
        val selectedFirst: Boolean = true,
        val importStrategy: AccessControlImportStrategy = AccessControlImportStrategy.MERGE,
        val needsMiuiPermission: Boolean = false,
        override val message: String? = null,
        override val error: String? = null,
    ) : LoadableState<UiState> {
        override fun withLoading(loading: Boolean): UiState = copy(isLoading = loading)

        override fun withError(error: String?): UiState = copy(error = error)

        override fun withMessage(message: String?): UiState = copy(message = message)
    }

    private val chinaAppDetector = ChinaAppDetector(application)

    val filteredApps: StateFlow<List<AppInfo>> =
        uiState
            .map { state ->
                filterApps(
                    apps = state.apps,
                    initialSelectedPackages = state.initialSelectedPackages,
                    query = state.searchQuery,
                    showSystemApps = state.showSystemApps,
                    sortMode = state.sortMode,
                    selectedFirst = state.selectedFirst,
                )
            }
            .stateInWhileSubscribed(viewModelScope, emptyList())

    sealed interface AccessControlUiEffect {
        data class ShowMessage(val message: String) : AccessControlUiEffect

        data class ShowError(val message: String) : AccessControlUiEffect
    }

    init {
        checkAndLoad()
    }

    private fun checkAndLoad() {
        val context = getApplication<Application>()
        val permission = "com.android.permission.GET_INSTALLED_APPS"

        if (RootPackageShell.hasRootAccess()) {
            loadApps()
            return
        }

        val hasPermission =
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            loadApps()
            return
        }

        val isMiui = runCatching {
            val permissionInfo = context.packageManager.getPermissionInfo(permission, 0)
            permissionInfo.packageName == "com.lbe.security.miui"
        }
            .getOrElse { false }

        if (isMiui) {
            _uiState.update { it.copy(needsMiuiPermission = true, isLoading = false) }
        } else {
            loadApps()
        }
    }

    fun onPermissionResult() {
        _uiState.update { it.copy(needsMiuiPermission = false) }
        loadApps()
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        controller.setAccessControlMode(mode)
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val selectedPackages = settings.accessControlPackages.value
            val apps = runCatching {
                withContext(Dispatchers.IO) { loadInstalledApps() }
            }
                .getOrElse {
                    _uiState.update { state ->
                        state.copy(isLoading = false, needsMiuiPermission = true)
                    }
                    return@launch
                }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    apps = apps,
                    selectedPackages = selectedPackages,
                    initialSelectedPackages = selectedPackages,
                )
            }
        }
    }

    /**
     * Synchronize selection-related state when the screen becomes visible again. The ViewModel is
     * activity-scoped by the Compose Koin owner, so navigating away does not recreate it.
     */
    fun refreshSelection() {
        val selectedPackages = settings.accessControlPackages.value
        _uiState.update { state ->
            state.copy(
                selectedPackages = selectedPackages,
                initialSelectedPackages = selectedPackages,
            )
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val selfPackageName = getApplication<Application>().packageName

        val packages = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }
            .getOrElse { error ->
                if (error is SecurityException) {
                    loadInstalledAppsFromRoot(pm, selfPackageName)
                } else {
                    throw error
                }
            }

        return packages
            .filter { it.packageName != selfPackageName }
            .map { appInfo ->
                val pkgInfo = runCatching { pm.getPackageInfo(appInfo.packageName, 0) }.getOrNull()
                AppInfo(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    installTime = pkgInfo?.firstInstallTime ?: 0L,
                    updateTime = pkgInfo?.lastUpdateTime ?: 0L,
                )
            }
    }

    private fun loadInstalledAppsFromRoot(
        pm: PackageManager,
        selfPackageName: String,
    ): List<ApplicationInfo> {
        val packageNames =
            RootPackageShell.queryInstalledPackageNames()
                ?: throw SecurityException("Unable to query installed packages from root shell")

        return packageNames
            .asSequence()
            .filterNot { it == selfPackageName }
            .mapNotNull { packageName ->
                runCatching { pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA) }
                    .getOrNull()
            }
            .toList()
    }

    private fun filterApps(
        apps: List<AppInfo>,
        initialSelectedPackages: Set<String>,
        query: String,
        showSystemApps: Boolean,
        sortMode: AccessControlSortMode,
        selectedFirst: Boolean,
    ): List<AppInfo> {
        val filtered = apps.filter { app ->
            val matchesQuery =
                query.isEmpty() ||
                        app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            val matchesSystemFilter = showSystemApps || !app.isSystemApp
            matchesQuery && matchesSystemFilter
        }
        val comparator =
            when (sortMode) {
                AccessControlSortMode.PACKAGE_NAME ->
                    compareBy<AppInfo> { it.packageName.lowercase() }

                AccessControlSortMode.LABEL -> compareBy { it.label.lowercase() }
                AccessControlSortMode.INSTALL_TIME -> compareBy { it.installTime }
                AccessControlSortMode.UPDATE_TIME -> compareBy { it.updateTime }
            }
        val sorted = filtered.sortedWith(comparator)
        return if (selectedFirst) {
            sorted.sortedByDescending { initialSelectedPackages.contains(it.packageName) }
        } else {
            sorted
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
    }

    fun onSortModeChange(mode: AccessControlSortMode) {
        settings.accessControlSortMode.set(mode)
        _uiState.update { state -> state.copy(sortMode = mode) }
    }

    fun onSelectedFirstChange(selectedFirst: Boolean) {
        settings.accessControlSelectedFirst.set(selectedFirst)
        _uiState.update { state -> state.copy(selectedFirst = selectedFirst) }
    }

    fun onImportStrategyChange(strategy: AccessControlImportStrategy) {
        settings.accessControlImportStrategy.set(strategy)
        _uiState.update { state -> state.copy(importStrategy = strategy) }
    }

    fun onShowSystemAppsChange(show: Boolean) {
        settings.accessControlShowSystemApps.set(show)
        _uiState.update { state -> state.copy(showSystemApps = show) }
    }

    fun onAppSelectionChange(packageName: String, selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                selectedPackages =
                    if (selected) {
                        state.selectedPackages + packageName
                    } else {
                        state.selectedPackages - packageName
                    }
            )
        }
        persistSelectionAndApply()
    }

    fun selectAll() {
        val currentFilteredPackages = filteredApps.value.mapTo(linkedSetOf()) { it.packageName }
        _uiState.update { state ->
            state.copy(selectedPackages = state.selectedPackages + currentFilteredPackages)
        }
        persistSelectionAndApply()
    }

    fun deselectAll() {
        val currentFilteredPackages = filteredApps.value.mapTo(linkedSetOf()) { it.packageName }
        _uiState.update { state ->
            state.copy(selectedPackages = state.selectedPackages - currentFilteredPackages)
        }
        persistSelectionAndApply()
    }

    fun invertSelection() {
        val currentFilteredPackages = filteredApps.value.mapTo(linkedSetOf()) { it.packageName }
        _uiState.update { state ->
            val newSelectedPackages = state.selectedPackages.toMutableSet()
            currentFilteredPackages.forEach { pkg ->
                if (!newSelectedPackages.add(pkg)) {
                    newSelectedPackages.remove(pkg)
                }
            }
            state.copy(selectedPackages = newSelectedPackages)
        }
        persistSelectionAndApply()
    }

    fun selectChinaAppsInCurrentList() = applyRegionalSelectionInCurrentList(selectChina = true)

    fun selectNonChinaAppsInCurrentList() = applyRegionalSelectionInCurrentList(selectChina = false)

    /**
     * China-app membership is computed on demand (FlClash-style deep scan behind
     * [ChinaAppDetector]'s cache): the first run dex-scans undecided APKs and can take a while, so
     * the list's loading state is raised for the duration.
     */
    private fun applyRegionalSelectionInCurrentList(selectChina: Boolean) {
        val currentFiltered = filteredApps.value
        if (currentFiltered.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val chinaPackages = runCatching {
                chinaAppDetector.detectChinaPackages(
                    currentFiltered.map {
                        ChinaAppDetector.Candidate(it.packageName, it.updateTime)
                    }
                )
            }
                .getOrElse {
                    _uiState.update { state -> state.copy(isLoading = false) }
                    return@launch
                }
            _uiState.update { state ->
                val currentPackages = currentFiltered.mapTo(linkedSetOf()) { it.packageName }
                val targetPackages =
                    currentFiltered
                        .filter { (it.packageName in chinaPackages) == selectChina }
                        .mapTo(linkedSetOf()) { it.packageName }
                state.copy(
                    isLoading = false,
                    selectedPackages =
                        state.selectedPackages.minus(currentPackages).plus(targetPackages),
                )
            }
            persistSelectionAndApply()
        }
    }

    fun exportPackages(): String = _uiState.value.selectedPackages.joinToString("\n")

    fun importPackages(text: String): Int {
        val packages = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val validPackages =
            packages.intersect(_uiState.value.apps.mapTo(linkedSetOf()) { it.packageName })

        _uiState.update { state ->
            state.copy(
                selectedPackages =
                    when (state.importStrategy) {
                        AccessControlImportStrategy.MERGE -> state.selectedPackages + validPackages
                        AccessControlImportStrategy.OVERWRITE -> validPackages
                    }
            )
        }

        persistSelectionAndApply()
        return validPackages.size
    }

    private fun persistSelectionAndApply() {
        controller.applyPackages(_uiState.value.selectedPackages)
    }
}
