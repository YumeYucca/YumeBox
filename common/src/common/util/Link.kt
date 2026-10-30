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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Starts [intent] and reports whether the system had something to start.
 *
 * Every launch can fail for the same reason: nothing on the device resolves the action, and
 * `startActivity` throws [ActivityNotFoundException] at the caller. Android has no way to ask in
 * advance, so the miss is logged here once instead of being handled — or crashing — at each call
 * site. Callers that show a waiting state must inspect the result and undo it.
 */
fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        if (this !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        true
    } catch (error: ActivityNotFoundException) {
        Timber.w(error, "No app can handle %s", intent.action)
        false
    }

/**
 * Opens [url] in whichever app the system resolves for it.
 *
 * A device that has no browser, or that has disabled the scheme, resolves nothing; a tapped link
 * must not take the app down with it, so [startActivitySafely] logs the miss.
 */
fun Context.openUrl(url: String) {
    startActivitySafely(Intent(Intent.ACTION_VIEW, url.toUri()))
}
