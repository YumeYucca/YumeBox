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

package com.github.yumeyucca.yumebox.common.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.yumeyucca.yumebox.R
import com.github.yumeyucca.yumebox.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DashboardShortcutHelper {
    /** Largest edge kept for a shortcut icon; launchers never draw it any bigger. */
    private const val MAX_ICON_EDGE_PX = 192

    suspend fun createPanelShortcut(
        context: Context,
        url: String,
        label: String,
        iconUri: Uri? = null,
    ) {
        if (url.isBlank()) return
        val icon =
            withContext(Dispatchers.IO) {
                iconUri?.let { loadIconFromUri(context, it) } ?: fetchFavicon(context, url)
            }
        val shortcut =
            ShortcutInfoCompat.Builder(context, "panel_" + url.hashCode())
                .setShortLabel(label.ifBlank { "Dashboard" })
                .setLongLabel(label.ifBlank { "Dashboard" })
                .setIcon(icon)
                .setIntent(WebViewActivity.intent(context, url))
                .build()
        withContext(Dispatchers.Main) {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            }
        }
    }

    private fun loadIconFromUri(context: Context, uri: Uri): IconCompat? {
        return runCatching {
            val resolver = context.contentResolver
            // A picked image can be a multi-megapixel photo; decoding it whole allocates tens of
            // megabytes and an OutOfMemoryError is an Error, so [runCatching] would not absorb it.
            // Measure first, then decode again with a power-of-two sample size.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                null
            } else {
                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize = iconSampleSize(bounds.outWidth, bounds.outHeight)
                    }
                resolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                        ?.let(IconCompat::createWithBitmap)
                }
            }
        }
            .getOrNull()
    }

    private fun iconSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (
            width / (sampleSize * 2) >= MAX_ICON_EDGE_PX &&
                height / (sampleSize * 2) >= MAX_ICON_EDGE_PX
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun fetchFavicon(context: Context, url: String): IconCompat {
        return runCatching {
            val u = java.net.URL(url)
            val faviconUrl = java.net.URL("${u.protocol}://${u.host}/favicon.ico")
            val conn =
                (faviconUrl.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                }
            try {
                conn.inputStream.use { input ->
                    val bytes = input.readBytes()
                    val bmp =
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?: return@runCatching null
                    IconCompat.createWithBitmap(bmp)
                }
            } finally {
                // Closing the stream alone leaves a failed (non-2xx) response's error stream queued
                // until the connection is collected; disconnect() releases the socket right away.
                conn.disconnect()
            }
        }
            .getOrNull() ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
    }
}
