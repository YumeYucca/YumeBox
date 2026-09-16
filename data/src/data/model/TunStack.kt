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

package com.github.yumeyucca.yumebox.data.model

enum class TunStack {
    System,
    GVisor,
    Mixed,
    /** Mihomo IP stack (MIPS): userspace replacement for gVisor. */
    Mips;

    /** Value written to mihomo `tun.stack`. */
    fun toCoreStack(): String =
        when (this) {
            System -> "system"
            GVisor -> "gvisor"
            Mixed -> "mixed"
            Mips -> "mips"
        }

    /**
     * VpnService attaches an app-owned TUN fd, so only userspace stacks work. System/Mixed need a
     * kernel TUN and fall back to gVisor.
     */
    fun forVpnService(): TunStack =
        when (this) {
            Mips -> Mips
            System, GVisor, Mixed -> GVisor
        }
}
