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

package com.github.yumeyucca.yumebox.runtime.service.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import tf.gal.yumebox.locale.YumeTxt

object RootAccessSupport {
    // tryResume/completeResume (internal API): the libsu shell callback may fire after the
    // caller was cancelled, so the resume must be race-safe rather than throw.
    @OptIn(InternalCoroutinesApi::class)
    suspend fun isGranted(): Boolean =
        suspendCancellableCoroutine { continuation ->
            fun resume(granted: Boolean) {
                val resumeToken = continuation.tryResume(granted) ?: return
                continuation.completeResume(resumeToken)
            }

            runCatching {
                Shell.getShell(Shell.EXECUTOR) { shell ->
                    resume(runCatching { shell.isRoot }.getOrDefault(false))
                }
            }
                .onFailure { resume(false) }
        }

    suspend fun require() {
        check(isGranted()) { YumeTxt.NetworkSettings.Error.RootRequired }
    }
}
