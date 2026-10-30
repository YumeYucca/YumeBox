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
import android.os.Handler
import android.os.Looper
import android.widget.Toast

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * The app's only toast: a system toast. Anything longer than a sentence belongs in a dialog the
 * screen owns, so there is no second, dialog-shaped toast path to keep alive.
 */
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isBlank()) return
    val ctx = applicationContext
    val show = { Toast.makeText(ctx, message, duration).show() }
    if (Looper.myLooper() == Looper.getMainLooper()) show() else mainHandler.post(show)
}
