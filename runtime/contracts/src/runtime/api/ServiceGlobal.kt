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

package com.github.yumeyucca.yumebox.runtime.api

import android.content.Context
import com.github.yumeyucca.yumebox.core.Global

// Backed by the single process-wide core.Global; every new process entry point
// (root service, content provider) must call this before touching packageName.
fun initializeServiceGlobal(app: Context) {
    Global.init(app)
}

val Context.appContextOrSelf: Context
    get() = applicationContext ?: this

val packageName: String
    get() = Global.application.packageName
