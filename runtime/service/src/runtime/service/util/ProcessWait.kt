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

package com.github.yumeyucca.yumebox.runtime.service.util

import android.os.SystemClock

/**
 * Blocks until [isAlive] reports `false` or [timeoutMs] elapses, polling every [pollMs].
 *
 * The core and preview children are stopped from a lifecycle lock, so these waits are
 * deliberately blocking: the caller must not return control while a child is still holding
 * the TUN interface or the port. Returns `true` when the child is gone.
 */
internal inline fun awaitProcessGone(
    timeoutMs: Long,
    pollMs: Long,
    isAlive: () -> Boolean,
): Boolean {
    val deadline = SystemClock.elapsedRealtime() + timeoutMs
    while (SystemClock.elapsedRealtime() < deadline) {
        if (!isAlive()) return true
        Thread.sleep(pollMs)
    }
    return !isAlive()
}
