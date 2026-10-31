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

import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.analytics.ANALYTICS_ENDPOINT
import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.data.controller.*
import com.github.yumeyucca.yumebox.data.analytics.AnonymousAnalyticsReporter
import com.github.yumeyucca.yumebox.data.store.*
import com.github.yumeyucca.yumebox.data.store.room.createTrafficStatisticsDao
import com.github.yumeyucca.yumebox.data.update.AppUpdateRepository
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

const val APPLICATION_SCOPE_NAME = "applicationScope"

/** One named MMKV storage: the Koin qualifier and the on-disk id come from the same argument. */
private fun Module.mmkv(id: String) {
    single<MMKV>(named(id)) { get<MMKVProvider>().getMMKV(id) }
}

val appFoundationModule = module {
    single<CoroutineScope>(named(APPLICATION_SCOPE_NAME)) {
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, error ->
                    Timber.e(error, "Uncaught failure in the application scope")
                },
        )
    }

    single { MMKVProvider() }
    mmkv("profiles")
    mmkv("settings")
    mmkv("network_settings")
    mmkv("substore")
    mmkv("proxy_display")
    mmkv("traffic_statistics")
    mmkv("profile_links")
    mmkv("service_cache")
    mmkv("override_bindings")
    mmkv("remote_controller")

    single { AppSettingsStore(get<MMKV>(named("settings"))) }
    single { NetworkSettingsStore(get(named("network_settings"))) }
    single { RemoteControllerStore(get(named("remote_controller"))) }
    single { ProfileLinksStore(get(named("profile_links"))) }
    single { FeatureStore(get(named("substore"))) }
    single { ProxyDisplaySettingsStore(get(named("proxy_display"))) }

    single { createTrafficStatisticsDao(androidApplication()) }
    single { TrafficStatisticsStore(get(named("traffic_statistics")), get()) }
}

val appDataRuntimeModule = module {
    single { AppUpdateRepository() }
    single {
        AnonymousAnalyticsReporter(
            mmkv = get<MMKVProvider>().getMMKV("settings"),
            endpoint = ANALYTICS_ENDPOINT,
            appVersion = BuildInfo.VERSION_NAME,
            versionCode = BuildInfo.VERSION_CODE,
        )
    }
    single { AppSettingsController(get(), applyLanguage = AppLanguageManager::apply) }
    single {
        val proxyFacade = get<ProxyFacade>()
        NetworkSettingsController(
            store = get(),
            isRunning = { proxyFacade.runtimeSnapshot.value.running },
            restartProxy = { mode -> proxyFacade.reloadProxy(mode) },
        )
    }
    single {
        val proxyFacade = get<ProxyFacade>()
        AccessControlController(
            store = get(),
            isRunning = { proxyFacade.isRunning.value },
            resolveActiveMode = {
                proxyFacade.runtimeSnapshot.value.let { snapshot ->
                    snapshot.runMode.takeIf {
                        snapshot.owner != RuntimeOwner.None &&
                            snapshot.owner != RuntimeOwner.RemoteController
                    }
                }
            },
            restartProxy = { mode -> proxyFacade.reloadProxy(mode) },
        )
    }
    single {
        val appContext = androidContext()
        ProvidersController(
            context = appContext,
            queryProvidersAction = {
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().queryProviders()
            },
            updateProviderAction = { type, name ->
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().updateProvider(type, name)
            },
        )
    }

    single { ProfileBindingStore(androidContext()) }
    single<ProfileBindingProvider> { get<ProfileBindingStore>() }

    single { OverrideConfigStore(androidContext(), get()) }
    single<OverrideConfigProvider> { get<OverrideConfigStore>() }

    single { OverrideResolver(get(), get()) }
    single { OverrideService(androidContext(), get()) }
    single {
        val profilesRepository = get<ProfilesRepository>()
        ActiveProfileOverrideReloader(
            queryActiveProfile = { profilesRepository.queryActiveProfile() },
            bindingProvider = get(),
            overrideService = get(),
        )
    }

    single { ProxyFacade(androidContext(), get(), get()) }
    single { AppIdentityResolver(androidContext()) }
    single { ProfilesRepository(androidContext()) }
    single {
        val proxyFacade = get<ProxyFacade>()
        AppTrafficStatisticsCollector(
            querySource =
                object : TrafficQuerySource {
                    override val isRunning: Flow<Boolean> = proxyFacade.isRunning

                    override fun currentProfileId(): String? =
                        proxyFacade.currentProfile.value?.uuid?.toString()

                    override suspend fun queryTrafficTotal(): TrafficData =
                        TrafficData.from(proxyFacade.queryTrafficTotal())

                    override suspend fun queryConnections(): ConnectionSnapshot =
                        proxyFacade.queryConnections()

                    override suspend fun queryActiveProfileId(): String? {
                        proxyFacade.refreshCurrentProfile()
                        return proxyFacade.currentProfile.value?.uuid?.toString()
                    }
                },
            trafficStatisticsStore = get(),
            appIdentityResolver = get(),
        )
    }
}

