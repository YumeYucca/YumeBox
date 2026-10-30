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

@file:Suppress("FunctionName", "SimplifiableCallChain")

package com.github.yumeyucca.yumebox.feature.editor.format

import com.github.yumeyucca.yumebox.core.model.LanguageScope
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object CodeFormatter {
    fun format(content: String, language: LanguageScope): String? =
        when (language) {
            LanguageScope.Json -> formatJson(content)
            LanguageScope.Yaml -> formatYaml(content)
            LanguageScope.JavaScript,
            LanguageScope.Text -> content
        }

    private fun formatJson(content: String): String? =
        try {
            val trimmed = content.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> null
            }
        } catch (_: JSONException) {
            null
        }

    /** `key: |` / `key: >-` and friends: everything indented below it is content, not layout. */
    private val BlockScalarHeader = Regex("^\\s*[^#\\s][^#]*:\\s*[|>][-+0-9]*\\s*(#.*)?$")

    /**
     * Whitespace-only tidying: trailing blanks and stacked empty lines, nothing structural.
     *
     * There is no YAML pretty-printer in this module on purpose — re-emitting a user's config
     * through a parser would drop comments and anchors, and the preview screen formats JSON
     * only. The name says "format" because that is what the menu action is called.
     *
     * A block scalar body is copied byte for byte: its trailing spaces and blank lines are the
     * value, so tidying them would rewrite the user's content.
     */
    private fun formatYaml(content: String): String? {
        val tidied = ArrayList<String>()
        var blockIndent = -1
        var blankRun = 0

        for (line in content.lines()) {
            val indent = line.indexOfFirst { !it.isWhitespace() }
            if (blockIndent >= 0) {
                if (line.isBlank() || indent > blockIndent) {
                    tidied += line
                    blankRun = 0
                    continue
                }
                blockIndent = -1
            }
            if (line.isBlank()) {
                blankRun++
                if (blankRun > 1) continue
                tidied += ""
                continue
            }
            blankRun = 0
            tidied += line.trimEnd()
            if (BlockScalarHeader.matches(line)) blockIndent = maxOf(indent, 0)
        }

        return tidied.joinToString("\n")
    }
}
