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

package com.github.yumeyucca.yumebox.runtime.service.session

import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.runtime.service.profile.SubscriptionUserAgent
import java.io.File

/**
 * Injects app-level `global-ua` into the compile override chain so core provider refresh uses the same User-Agent as
 * Kotlin subscription downloads. Always appended last so it beats subscription and user override values; not gated by
 * disable-all.
 */
object GlobalUaOverride {
    const val FILE_NAME = "__global_ua_override__.yaml"

    fun buildYaml(userAgent: String = SubscriptionUserAgent.resolve()): String {
        val escaped = userAgent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        return "global-ua: \"$escaped\"\n"
    }

    fun materialize(
        dir: File,
        userAgent: String = SubscriptionUserAgent.resolve(),
    ): OverrideSpec {
        dir.mkdirs()
        val file = File(dir, FILE_NAME)
        file.writeText(buildYaml(userAgent))
        return OverrideSpec(path = file.absolutePath, ext = "yaml")
    }
}
