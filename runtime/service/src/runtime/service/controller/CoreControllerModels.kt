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

package com.github.yumeyucca.yumebox.runtime.service.controller

import com.github.yumeyucca.yumebox.core.model.TunnelState
import kotlinx.serialization.Serializable

@Serializable internal data class RawProvidersResponse(val providers: Map<String, RawProvider> = emptyMap())

@Serializable
internal data class RawProvider(
    val name: String = "",
    val vehicleType: String = "",
    val updatedAt: String? = null,
    val proxies: List<RawProxy> = emptyList(),
)

@Serializable internal data class RawProxiesResponse(val proxies: Map<String, RawProxy> = emptyMap())

@Serializable internal data class RawGroupResponse(val proxies: List<RawProxy> = emptyList())

@Serializable
internal data class RawProxy(
    val name: String,
    val type: String,
    val now: String = "",
    val all: List<String> = emptyList(),
    val history: List<RawDelay> = emptyList(),
    val hidden: Boolean = false,
    val icon: String? = null,
    val udp: Boolean = false,
)

@Serializable internal data class RawDelay(val delay: Int = 0)

@Serializable internal data class RawDelayResult(val delay: Int = 0)

@Serializable internal data class RawConfigs(val mode: TunnelState.Mode = TunnelState.Mode.Rule)

@Serializable
internal data class RawTraffic(
    val up: Long = 0,
    val down: Long = 0,
    val upTotal: Long = 0,
    val downTotal: Long = 0,
)

@Serializable internal data class SelectBody(val name: String)

@Serializable internal data class RawRulesResponse(val rules: List<RawRule> = emptyList())

@Serializable
internal data class RawRule(
    val index: Int = 0,
    val type: String = "",
    val payload: String = "",
    val proxy: String = "",
    val size: Int = -1,
    val extra: RawRuleExtra? = null,
)

@Serializable
internal data class RawRuleExtra(
    val disabled: Boolean = false,
    val hitCount: Long = 0L,
    val missCount: Long = 0L,
)

internal data class TimedTrafficSample(
    val sample: RawTraffic,
    val capturedAtNanos: Long,
)

internal data class ProviderSnapshot(
    val nodes: Map<String, RawProxy>,
    val owners: Map<String, String>,
) {
    companion object {
        val Empty = ProviderSnapshot(emptyMap(), emptyMap())
    }
}

internal data class TimedProviderSnapshot(
    val snapshot: ProviderSnapshot,
    val capturedAtNanos: Long,
)
