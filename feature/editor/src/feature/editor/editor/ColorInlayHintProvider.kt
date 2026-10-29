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
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.feature.editor.editor

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.lang.styling.inlayHint.ColorInlayHint
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintProvider
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.widget.CodeEditor

internal class ColorInlayHintProvider(
    private val editor: CodeEditor,
) : InlayHintProvider {
    override fun provideInlayHints(container: InlayHintsContainer) {
        val content = editor.text
        for (line in 0 until content.lineCount) {
            HexColorPattern.findAll(content.getLineString(line)).forEach { match ->
                val color = parseCssColor(match.value) ?: return@forEach
                container.add(
                    ColorInlayHint(
                        line = line,
                        column = match.range.last + 1,
                        color = ResolvableColor { color },
                    )
                )
            }
        }
    }

    private fun parseCssColor(value: String): Int? {
        val hex = value.removePrefix("#")
        val expanded =
            when (hex.length) {
                3 -> hex.map { "$it$it" }.joinToString("") + "FF"
                4 -> hex.map { "$it$it" }.joinToString("")
                6 -> "$hex" + "FF"
                8 -> hex
                else -> return null
            }
        val rgb = expanded.substring(0, 6).toLong(16).toInt()
        val alpha = expanded.substring(6, 8).toLong(16).toInt()
        return (alpha shl 24) or rgb
    }

    private companion object {
        val HexColorPattern =
            Regex(
                pattern = """(?i)#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})(?![0-9a-f])""",
            )
    }
}
