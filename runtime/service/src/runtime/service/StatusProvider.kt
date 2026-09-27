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

package com.github.yumeyucca.yumebox.runtime.service

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.initializeServiceGlobal
import com.tencent.mmkv.MMKV

/**
 * Runs before `Application.onCreate`: initializes MMKV and pre-creates the stores the root daemon
 * shares with the app. Runtime state itself lives in [RuntimeCoordinator].
 */
class StatusProvider : ContentProvider() {
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? =
        when (method) {
            METHOD_CURRENT_PROFILE ->
                if (RuntimeCoordinator.state.value.phase == RuntimePhase.Running) {
                    Bundle().apply { putString("name", currentProfile) }
                } else {
                    null
                }

            else -> super.call(method, arg, extras)
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw IllegalArgumentException("Stub!")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw IllegalArgumentException("Stub!")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw IllegalArgumentException("Stub!")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw IllegalArgumentException("Stub!")

    override fun getType(uri: Uri): String? = throw IllegalArgumentException("Stub!")

    override fun onCreate(): Boolean {
        runCatching {
            val app = context?.applicationContext as? Application ?: return@runCatching
            initializeServiceGlobal(app)
            MMKV.initialize(app)
            // A store first created by the root uid would be root-owned and unusable by the app.
            listOf("service", "network_settings", "profiles", "root_tun_state").forEach { id ->
                runCatching { MMKV.mmkvWithID(id, MMKV.MULTI_PROCESS_MODE) }
            }
            legacyRuntimeFiles.forEach { name ->
                runCatching { app.filesDir.resolve(name).delete() }
            }
        }
        return true
    }

    companion object {
        const val METHOD_CURRENT_PROFILE = "currentProfile"

        private val legacyRuntimeFiles =
            listOf("service_running.lock", "service_autostart.lock", "service_running_mode.txt")

        @Volatile var currentProfile: String? = null
    }
}

