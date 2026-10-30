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

package com.github.yumeyucca.yumebox.core.util

import kotlinx.serialization.json.Json

/**
 * Parsers for JSON this app does not fully own: HTTP bodies and config files shipped by the
 * core. Unknown fields are ignored so a newer payload cannot break an older build.
 */
val LenientJson: Json = Json { ignoreUnknownKeys = true }

/**
 * The policy for JSON this app *persists* (MMKV blobs, imported profiles, compile requests).
 *
 * [JsonBuilder.encodeDefaults] keeps a round-trip byte-stable, so a re-save cannot drop a field
 * a reader depends on. `coerceInputValues` degrades an unknown enum/`null`-for-non-null value to
 * the declared default instead of throwing, which is what a hand-edited profile file needs.
 */
val PersistedJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

/** [PersistedJson] with the readable-on-disk shape used for generated YAML/JSON artefacts. */
val PrettyPersistedJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        explicitNulls = false
        prettyPrint = true
    }
