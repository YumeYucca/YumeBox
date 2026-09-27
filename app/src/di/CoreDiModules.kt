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
import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.data.controller.*
import com.github.yumeyucca.yumebox.data.network.NetworkInfoService
import com.github.yumeyucca.yumebox.data.store.*
import com.github.yumeyucca.yumebox.data.store.room.createTrafficStatisticsDao
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val APPLICATION_SCOPE_NAME = "applicationScope"

val appFoundationModule = module {
    single<CoroutineScope>(named(APPLICATION_SCOPE_NAME)) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single { MMKVProvider() }
    single<MMKV>(named("profiles")) { get<MMKVProvider>().getMMKV("profiles") }
    single<MMKV>(named("settings")) { get<MMKVProvider>().getMMKV("settings") }
    single<MMKV>(named("network_settings")) { get<MMKVProvider>().getMMKV("network_settings") }
    single<MMKV>(named("substore")) { get<MMKVProvider>().getMMKV("substore") }
    single<MMKV>(named("proxy_display")) { get<MMKVProvider>().getMMKV("proxy_display") }
    single<MMKV>(named("traffic_statistics")) { get<MMKVProvider>().getMMKV("traffic_statistics") }
    single<MMKV>(named("profile_links")) { get<MMKVProvider>().getMMKV("profile_links") }
    single<MMKV>(named("service_cache")) { get<MMKVProvider>().getMMKV("service_cache") }
    single<MMKV>(named("override_bindings")) { get<MMKVProvider>().getMMKV("override_bindings") }
    single<MMKV>(named("remote_controller")) { get<MMKVProvider>().getMMKV("remote_controller") }

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
    single { AppSettingsController(get(), applyLanguage = AppLanguageManager::apply) }
    single {
        val proxyFacade = get<ProxyFacade>()
        NetworkSettingsController(
            store = get(),
            isRunning = { RuntimeStateMapper.isActuallyRunning(proxyFacade.runtimeSnapshot.value) },
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
    single { NetworkInfoService() }
    single {
        val profilesRepository = get<ProfilesRepository>()
        RuntimeOverrideController(
            configStore = get(),
            queryActiveProfile = { profilesRepository.queryActiveProfile() },
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

val coreDiModules: List<Module> = listOf(appFoundationModule, appDataRuntimeModule)
