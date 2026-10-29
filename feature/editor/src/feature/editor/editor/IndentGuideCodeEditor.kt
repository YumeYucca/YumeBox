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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorRenderer

internal class IndentGuideCodeEditor(context: Context) : CodeEditor(context) {
    var indentGuideColor: Int = 0
    var activeIndentGuideColor: Int = 0

    private val guidePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
        }

    override fun onCreateRenderer(): EditorRenderer =
        object : EditorRenderer(this@IndentGuideCodeEditor) {
            override fun draw(canvas: Canvas) {
                super.draw(canvas)
                drawIndentGuides(canvas)
            }
        }

    private fun drawIndentGuides(canvas: Canvas) {
        if (isWordwrap || (indentGuideColor == 0 && activeIndentGuideColor == 0)) return

        val content = text
        val lineCount = content.lineCount
        if (lineCount == 0) return

        val firstLine = firstVisibleLine.coerceIn(0, lineCount - 1)
        val lastLine = lastVisibleLine.coerceIn(firstLine, lineCount - 1)
        val activeLine = getCursor().leftLine
        val tab = tabWidth.coerceAtLeast(1)
        val density = resources.displayMetrics.density
        guidePaint.strokeWidth = density

        val saveCount = canvas.save()
        canvas.clipRect(
            measureTextRegionOffset(),
            0f,
            width.toFloat(),
            height.toFloat(),
        )

        for (line in firstLine..lastLine) {
            val lineText = content.getLineString(line)
            var indentColumns = 0
            for (char in lineText) {
                when (char) {
                    ' ' -> indentColumns++
                    '\t' -> indentColumns += tab - (indentColumns % tab)
                    else -> break
                }
            }
            if (indentColumns < tab) continue

            val top = (getRowTop(line) - offsetY).toFloat()
            val bottom = (getRowBottom(line) - offsetY).toFloat()
            if (bottom < 0f || top > height) continue

            val color =
                if (!getCursor().isSelected() && line == activeLine && activeIndentGuideColor != 0) {
                    activeIndentGuideColor
                } else {
                    indentGuideColor
                }
            if (color == 0) continue
            guidePaint.color = color

            var column = tab
            while (column <= indentColumns) {
                val x = getCharOffsetX(line, column)
                if (x >= 0f && x <= width) {
                    canvas.drawLine(x, top, x, bottom, guidePaint)
                }
                column += tab
            }
        }

        canvas.restoreToCount(saveCount)
    }
}
