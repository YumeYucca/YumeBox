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

@file:Suppress("UnusedSymbol", "SimplifiableCallChain")

package com.github.yumeyucca.yumebox.runtime.service.util

import android.content.Context
import android.content.Intent
import com.github.yumeyucca.yumebox.runtime.api.Intents
import java.io.File
import java.util.*

val Context.importedDir: File
    get() = filesDir.resolve("imported")

val File.directoryLastModified: Long?
    get() {
        return walk().map { it.lastModified() }.maxOrNull()
    }

fun Context.sendBroadcastSelf(intent: Intent) {
    sendBroadcast(intent.setPackage(this.packageName))
}

fun Context.sendProfileChanged(uuid: UUID, affectsRuntime: Boolean) {
    val intent =
        Intent(Intents.ACTION_PROFILE_CHANGED)
            .putExtra(Intents.EXTRA_UUID, uuid.toString())
            .putExtra(Intents.EXTRA_AFFECTS_RUNTIME, affectsRuntime)

    sendBroadcastSelf(intent)
}

fun Context.sendOverrideChanged() {
    sendBroadcastSelf(Intent(Intents.ACTION_OVERRIDE_CHANGED))
}
