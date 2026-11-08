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

package com.github.yumeyucca.yumebox.core.bridge

import androidx.annotation.Keep

/**
 * The config compiler (liboverride, Rust). Compiles a profile + its override chain into the complete final mihomo
 * config, in-process. This is the only surviving native binding on the app side: the core itself runs out-of-process,
 * so the compiled config is streamed to it — nothing is loaded into this process.
 */
@Keep
object Compiler {
    init {
        System.loadLibrary("override")
    }

    /** Compile [requestJson] (a CompileRequest); returns a CompileResult JSON with `finalYaml`. */
    external fun nativeCompile(requestJson: String): String

    /** Generate an age x25519 key pair; returns JSON `{secretKey, publicKey}`. */
    external fun nativeGenAgeKey(): String

    /** Derive the age public key for [secretKey], or "" if it does not parse. */
    external fun nativeAgePublicKey(secretKey: String): String
}
