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

package com.github.yumeyucca.yumebox.runtime.client.access

import android.content.Context
import com.github.yumeyucca.yumebox.data.store.MMKVProvider
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.api.ProfileApi
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.api.initializeServiceGlobal
import com.github.yumeyucca.yumebox.runtime.service.controller.CoreController
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.profile.ProfileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

object RuntimeAccess {
    private val mutex = Mutex()
    private var coreApi: CoreApi? = null
    private var profileApi: ProfileApi? = null
    private var remoteApi: CoreController? = null

    private fun ensureRemoteController(): CoreController =
        remoteApi
            ?: RemoteControllerStore(MMKVProvider().getMMKV(RemoteControllerStore.MMKV_ID))
                .let { store -> CoreController(backendProvider = { store.activeBackend() }) }
                .also { remoteApi = it }

    suspend fun connect(ctx: Context) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (coreApi != null && profileApi != null) return@withLock
                val appContext = ctx.appContextOrSelf
                runCatching {
                    initializeServiceGlobal(appContext)
                    coreApi =
                        CoreRouter(
                            local = CoreProcess.controller(appContext),
                            remote = ensureRemoteController(),
                            isRemoteControllerActive = { RemoteControllerStore.isActive() },
                        )
                    profileApi = ProfileService(appContext)
                }
                    .onFailure { error ->
                        coreApi = null
                        profileApi = null
                        if (error !is CancellationException) Timber.e(error, "RuntimeAccess init failed")
                    }
                    .getOrThrow()
            }
        }
    }

    /**
     * Probe the configured remote backend without routing through [CoreRouter]. Safe to call before
     * [connect]; does not attach local unix-socket state.
     */
    suspend fun probeRemoteController(): Boolean =
        withContext(Dispatchers.IO) {
            val remote = mutex.withLock { ensureRemoteController() }
            remote.probe()
        }

    fun core(): CoreApi = checkNotNull(coreApi) { "RuntimeAccess not connected" }

    fun profile(): ProfileApi = checkNotNull(profileApi) { "RuntimeAccess not connected" }
}
