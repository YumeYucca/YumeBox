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

/**
 * The top-level `secret:` a compiled config carries, or "" when the profile sets none.
 *
 * The app never mints or rewrites a bearer: the profile's own secret is what the core enforces, and a profile without
 * one launches a controller that asks for no auth. The local controller is a unix socket in the app-owned runtime
 * directory, so an empty bearer is not reachable by other apps; only a user-configured remote controller needs a secret
 * of its own.
 */
internal fun controllerSecretFromConfig(yaml: String): String {
    val raw =
        yaml
            .lineSequence()
            .firstOrNull { it.trimStart().startsWith("secret:") }
            ?.substringAfter("secret:")
            ?.trim()
            ?: return ""
    return raw.trim('"', '\'').trim()
}
