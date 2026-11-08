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

package com.github.yumeyucca.yumebox.core.bridge

/**
 * Loads `libcompat.so`, the out-of-process core bridge (fork+exec, socketpair channel with SCM_RIGHTS fd passing,
 * UNIX-domain controller socket). The library is tiny (~10 KB) and always shipped raw in the APK's lib dir, so a plain
 * [System.loadLibrary] is sufficient — it never needs the XZ compressed-lib path that the heavyweight core libraries
 * use.
 */
internal object CompatNative {
    @Volatile private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            System.loadLibrary("compat")
            loaded = true
        }
    }
}
