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

package com.github.yumeyucca.yumebox.common.util

import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.tencent.mmkv.MMKV

/**
 * DI-resolved collaborators for [ProxyAutoStartHelper.checkAndAutoStart], bundled so the call site resolves them once
 * and provides them as a single context value.
 */
class AutoStartDependencies(
    val featureStore: FeatureStore,
    val proxyFacade: ProxyFacade,
    val profilesRepository: ProfilesRepository,
    val appSettingsStorage: AppSettingsStore,
    val networkSettingsStorage: NetworkSettingsStore,
    val serviceCache: MMKV,
)
