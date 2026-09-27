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

package com.github.yumeyucca.yumebox.runtime.service.core

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.runtime.api.CoreEndpointRef
import com.github.yumeyucca.yumebox.runtime.api.ProcessController
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.session.RuntimeServiceLauncher

/** Android binding of [ProcessController] over [CoreProcess] static ownership. */
class AndroidProcessController(context: Context) : ProcessController {
    private val appContext = context.appContextOrSelf

    override fun currentEndpoint(): CoreEndpointRef? =
        CoreProcess.current?.let { CoreEndpointRef(sock = it.sock, secret = it.secret) }

    override fun stop() {
        // Stop the service instead of killing its child directly: VpnTunTransport gives mihomo a
        // bounded SIGTERM window to persist selector state before escalating to SIGKILL.
        RuntimeServiceLauncher.stop(appContext)
    }

    override fun stopRoot() {
        CoreProcess.stopRoot()
    }

    override fun isRootDaemonAlive(): Boolean = CoreProcess.isRootDaemonAlive()

    override fun rootDaemonMode(): RunMode? = CoreProcess.rootDaemonMode()

    override fun reconnectRoot(): String? = CoreProcess.reconnectRoot(appContext)
}
