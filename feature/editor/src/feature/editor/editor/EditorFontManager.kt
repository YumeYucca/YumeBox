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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.feature.editor.editor

import android.content.Context
import android.graphics.Typeface
import com.github.yumeyucca.yumebox.feature.editor.R
import timber.log.Timber

object EditorFontManager {
    private var cachedFont: Typeface? = null

    @Suppress("TooGenericExceptionCaught")
    fun getEditorTypeface(context: Context): Typeface =
        cachedFont
            ?: try {
                context.resources.getFont(R.font.jetbrains_mono_regular).also {
                    cachedFont = it
                    Timber.d("JetBrainsMono font loaded successfully")
                }
            } catch (error: Exception) { // fault barrier: createFromAsset throws undocumented
                // RuntimeException
                Timber.w(error, "Failed to load JetBrainsMono font, falling back to MONOSPACE")
                Typeface.MONOSPACE
            }
}
