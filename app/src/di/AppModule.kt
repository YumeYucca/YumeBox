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

package com.github.yumeyucca.yumebox.di

import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.screen.log.LogViewModel
import com.github.yumeyucca.yumebox.screen.profiles.ProfilesViewModel
import com.github.yumeyucca.yumebox.screen.rules.RulesViewModel
import com.github.yumeyucca.yumebox.screen.settings.AccessControlViewModel
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import com.github.yumeyucca.yumebox.screen.settings.NetworkSettingsViewModel
import com.github.yumeyucca.yumebox.screen.settings.RemoteControllerViewModel
import com.github.yumeyucca.yumebox.screen.settings.WifiAutomationViewModel
import com.github.yumeyucca.yumebox.screen.settings.backup.BackupRepository
import com.github.yumeyucca.yumebox.screen.settings.backup.BackupRestoreViewModel
import com.github.yumeyucca.yumebox.screen.settings.backup.BackupStoreAdapter
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appIntegrationModule = module {
    single {
        BackupStoreAdapter(
            appSettings = get(),
            networkSettings = get(),
            featureSettings = get(),
            proxyDisplaySettings = get(),
            profileLinks = get(),
            remoteController = get(),
            proxyFacade = get(),
            mmkvProvider = get(),
        )
    }
    single {
        BackupRepository(
            application = androidApplication(),
            proxyFacade = get(),
            storeAdapter = get(),
        )
    }
}

val appViewModelModule = module {
    viewModel { AppSettingsViewModel(androidApplication(), get(), get(), get()) }
    viewModel { HomeViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ProfilesViewModel(androidApplication(), get(), get()) }
    viewModel { NetworkSettingsViewModel(androidApplication(), get(), get()) }
    viewModel { RemoteControllerViewModel(androidApplication(), get(), get()) }
    viewModel { AccessControlViewModel(androidApplication(), get(), get()) }
    viewModel { WifiAutomationViewModel(androidApplication(), get(), get()) }
    viewModel { LogViewModel(androidApplication()) }
    viewModel { RulesViewModel(androidApplication()) }
    viewModel { BackupRestoreViewModel(androidApplication(), get()) }
}

val appModule: List<Module> =
    coreDiModules +
            listOf(appIntegrationModule, appViewModelModule) +
            featureSubStoreModules +
            featureProxyModules +
            featureOverrideModules +
            featureMetaModules
