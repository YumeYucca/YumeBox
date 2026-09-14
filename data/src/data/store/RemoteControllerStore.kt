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

@file:Suppress("RemoveExplicitTypeArguments")

package com.github.yumeyucca.yumebox.data.store

import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.tencent.mmkv.MMKV

/**
 * Local runtime that was paused so the app could attach to an external controller. Names are stored
 * as strings so this store stays independent of runtime-api types.
 */
data class PausedLocalRuntime(
    val ownerName: String,
    val modeName: String,
)

/**
 * Persists external-controller mode state: whether the app should act as a pure remote controller,
 * the list of saved backends, and which backend is active.
 *
 * Stored in its own MMKV file (`remote_controller`) using [MMKV.MULTI_PROCESS_MODE] so both the UI
 * and service processes observe the same configuration.
 *
 * [controllerEnabled] is the user preference. The app only takes over as a remote controller when
 * [isActive] is true (preference on, backend selected, and the backend is currently attached).
 */
class RemoteControllerStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {
    private val backendCodec = RemoteBackendStorageCodec()

    /**
     * Master switch — when on (and an active backend exists) the app *wants* remote-controller
     * mode. Takeover still requires a reachable backend; see [isActive].
     */
    val controllerEnabled by boolFlow(false)

    /**
     * True only while the session is actually attached to a reachable remote backend. Service
     * processes read this via [isActive] so they do not skip local start when the preference is on
     * but the controller is down.
     */
    val controllerAttached by boolFlow(false)

    val pausedLocalOwner by strFlow("")
    val pausedLocalMode by strFlow("")

    /** All saved backends. Secrets are encrypted at this persistence boundary. */
    val backends by
    jsonListFlow(
        default = emptyList<RemoteBackend>(),
        decode = { str -> backendCodec.decode(this, str) },
        encode = { value -> backendCodec.encode(this, value) },
    )

    init {
        val persisted = mmkv.decodeString("backends").orEmpty()
        if (Regex("\"secret\"\\s*:").containsMatchIn(persisted)) {
            backends.set(backends.value)
        }
    }

    /** Id of the currently active backend, or blank if none selected. */
    val activeBackendId by strFlow("")

    /** Convenience: resolve the active [RemoteBackend], or null if unset / missing. */
    fun activeBackend(): RemoteBackend? {
        val id = activeBackendId.value
        if (id.isBlank()) return null
        return backends.value.firstOrNull { it.id == id }
    }

    /** Preference is on and a backend is selected — does not mean we have taken over. */
    fun isWanted(): Boolean = controllerEnabled.value && activeBackend() != null

    /** Currently attached to a reachable remote controller. */
    fun isActive(): Boolean = isWanted() && controllerAttached.value

    fun rememberPausedLocal(
        ownerName: String,
        modeName: String,
    ) {
        if (pausedLocalOwner.value.isNotBlank()) return
        pausedLocalOwner.set(ownerName)
        pausedLocalMode.set(modeName)
    }

    fun takePausedLocal(): PausedLocalRuntime? {
        val ownerName = pausedLocalOwner.value
        val modeName = pausedLocalMode.value
        if (ownerName.isBlank() || modeName.isBlank()) return null
        pausedLocalOwner.set("")
        pausedLocalMode.set("")
        return PausedLocalRuntime(ownerName = ownerName, modeName = modeName)
    }

    companion object {
        const val MMKV_ID = "remote_controller"

        private val gate by lazy { RemoteControllerStore(MMKVProvider().getMMKV(MMKV_ID)) }

        fun isActive(): Boolean = gate.isActive()
    }
}
