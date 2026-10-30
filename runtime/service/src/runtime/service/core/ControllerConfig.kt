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

package com.github.yumeyucca.yumebox.runtime.service.core

import java.util.*

/** Compiled config together with the controller bearer its core will actually enforce. */
internal class ControllerConfig(
    val yaml: String,
    val secret: String,
)

/** Mints a bearer the controller accepts; never empty, so REST auth stays consistent. */
internal fun newControllerSecret(): String = UUID.randomUUID().toString().replace("-", "")

/**
 * Rewrites the top-level `secret:` of [yaml] to [secret] (or adds one) so the launched core and
 * the app agree on the bearer without exposing it in argv.
 */
internal fun patchControllerSecret(yaml: String, secret: String = newControllerSecret()): ControllerConfig {
    val line = "secret: \"$secret\""
    val lines = yaml.lineSequence().toMutableList()
    val index = lines.indexOfFirst { it.trimStart().startsWith("secret:") }
    if (index >= 0) {
        lines[index] = line
    } else {
        lines.add(0, line)
    }
    return ControllerConfig(lines.joinToString("\n"), secret)
}
