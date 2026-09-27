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

import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.enumByNameOrNull
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.tencent.mmkv.MMKV

/**
 * The last startup failure, kept for ten minutes so a restarted app can still say why the core
 * is down. Only a Failed record is meaningful; anything else found under these keys is a
 * leftover of the old persisted phase slot and is dropped on read.
 */
internal object RuntimeFailureRecord {
    data class Failure(val mode: RunMode, val error: String)

    fun load(): Failure? {
        val cache = cache()
        LEGACY_KEYS.forEach(cache::removeValueForKey)
        val phase = cache.decodeString(KEY_PHASE)
        val mode = enumByNameOrNull<RunMode>(cache.decodeString(KEY_MODE))
        val error = cache.decodeString(KEY_LAST_ERROR)?.takeIf(String::isNotBlank)
        val age = System.currentTimeMillis() - cache.decodeLong(KEY_AT, 0L)
        if (phase == RuntimePhase.Failed.name && mode != null && error != null && age in 0..RETENTION_MS) {
            return Failure(mode, error)
        }
        clear()
        return null
    }

    fun save(mode: RunMode, error: String) {
        cache().apply {
            encode(KEY_MODE, mode.name)
            encode(KEY_PHASE, RuntimePhase.Failed.name)
            encode(KEY_AT, System.currentTimeMillis())
            encode(KEY_LAST_ERROR, error)
        }
    }

    fun clear() {
        cache().apply {
            removeValueForKey(KEY_MODE)
            removeValueForKey(KEY_PHASE)
            removeValueForKey(KEY_AT)
            removeValueForKey(KEY_LAST_ERROR)
        }
    }

    private fun cache(): MMKV = MMKV.mmkvWithID(SERVICE_CACHE_ID, MMKV.MULTI_PROCESS_MODE)

    private const val SERVICE_CACHE_ID = "service_cache"
    private const val KEY_MODE = "local_runtime_mode"
    private const val KEY_PHASE = "local_runtime_phase"
    private const val KEY_AT = "local_runtime_started_at"
    private const val KEY_LAST_ERROR = "local_runtime_last_error"
    private val LEGACY_KEYS = listOf("local_runtime_session", "local_tun_starting")
    private const val RETENTION_MS = 10 * 60_000L
}
