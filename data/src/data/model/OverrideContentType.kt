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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OverrideContentType(
    val extension: String,
    val displayName: String,
    /** Legacy extensions still accepted when reading existing files. */
    val previousExtensions: List<String> = emptyList(),
) {
    @SerialName("yaml")
    Yaml("yaml", "YAML", previousExtensions = listOf("yml")),
    @SerialName("js")
    JavaScript("js", "JavaScript");

    companion object {
        /** Every filename extension an override config file may use. */
        val knownExtensions: Set<String> =
            entries.flatMapTo(linkedSetOf()) { listOf(it.extension) + it.previousExtensions }

        fun fromExtension(extension: String?): OverrideContentType? {
            val normalized = extension?.lowercase()?.removePrefix(".") ?: return null
            return entries.firstOrNull {
                normalized == it.extension || normalized in it.previousExtensions
            }
        }

        fun fromFileName(fileName: String?): OverrideContentType? =
            fromExtension(fileName?.substringAfterLast('.', missingDelimiterValue = ""))
    }
}
