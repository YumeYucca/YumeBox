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

/**
 * A dashboard the Sub-Store panel can host.
 *
 * The selection is persisted as [ordinal] (`FeatureStore.selectedPanelType`), so entries must never
 * be reordered or removed without migrating the stored value.
 */
enum class SubStorePanel(
    val displayName: String,
    private val host: String,
) {
    Zashboard("Zashboard", "board.zash.run.place"),
    MetaCubeX("MetaCubeXD", "metacubex.github.io/metacubexd"),
    Yacd("Yacd", "yacd.haishan.me"),
    ;

    fun url(protocol: RemoteProtocol = RemoteProtocol.HTTPS): String = "${protocol.scheme}://$host"

    companion object {
        fun fromIndex(index: Int): SubStorePanel? = entries.getOrNull(index)
    }
}
