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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.core.model


enum class LanguageScope(
    val scopeName: String,
    val displayName: String,
) {
    Yaml("source.yaml", "YAML"),
    JavaScript("source.js", "JavaScript"),
    Json("source.json", "JSON"),
    Text("text.plain", "Plain Text");

    companion object {
        fun fromExtension(extension: String): LanguageScope =
            when (extension.lowercase()) {
                "yaml",
                "yml" -> Yaml

                "js" -> JavaScript
                "json" -> Json
                else -> Text
            }

        fun fromScopeName(scopeName: String): LanguageScope =
            entries.find { it.scopeName == scopeName } ?: Text
    }
}
