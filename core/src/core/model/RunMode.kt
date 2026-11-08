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

package com.github.yumeyucca.yumebox.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The single proxy run-mode key shared across UI, runtime, launcher, owner, and compiler. [VpnService] is the non-root
 * Android VpnService path; [Tun] and [Ebpf] are root libsu-daemon paths. The serialized names are the contract with the
 * core's `--mode` and the compiler's `run_mode`.
 */
@Serializable
enum class RunMode {
    @SerialName("vpn") VpnService,
    @SerialName("tun") Tun,
    @SerialName("ebpf") Ebpf;

    /** The token this mode maps to for the native core's `--mode` flag and the persisted daemon record. */
    val coreArg: String
        get() =
            when (this) {
                VpnService -> "vpn"
                Tun -> "tun"
                Ebpf -> "ebpf"
            }

    companion object {
        fun fromCoreArg(value: String?): RunMode? =
            when (value) {
                "vpn" -> VpnService
                "tun" -> Tun
                "ebpf" -> Ebpf
                else -> null
            }
    }
}
