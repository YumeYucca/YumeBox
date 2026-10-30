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

package com.github.yumeyucca.yumebox.presentation.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes the launcher icon of [packageName] at [sizePx] square pixels.
 *
 * Blocking: callers must be off the main thread. Returns `null` for a blank or unknown package —
 * a missing icon is a cosmetic fallback, so callers draw their own placeholder instead of failing.
 */
fun resolveAppIconBitmap(context: Context, packageName: String?, sizePx: Int): ImageBitmap? {
    val target = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        context.packageManager
            .getApplicationIcon(target)
            .toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
    }.getOrNull()
}

/**
 * [resolveAppIconBitmap] as remembered state: decodes once per ([packageName], [sizePx]) pair off the
 * main thread, so a list row never re-reads the package manager while scrolling.
 */
@Composable
fun rememberAppIconBitmap(packageName: String?, sizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val icon by
        produceState<ImageBitmap?>(initialValue = null, key1 = packageName, key2 = sizePx) {
            value = withContext(Dispatchers.IO) { resolveAppIconBitmap(context, packageName, sizePx) }
        }
    return icon
}
