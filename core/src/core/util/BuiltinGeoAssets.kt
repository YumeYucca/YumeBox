/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.core.util

import android.content.Context

class BuiltinGeoAssetsRequiredException : IllegalStateException()

/** Blocks local core startup until an MMDB Geo database has been installed once. */
fun Context.requireBuiltinGeoAssets() {
    val installed =
        runtimeHomeDir.listFiles()?.any { file ->
            file.isFile &&
                file.length() > 0L &&
                file.extension.equals("mmdb", ignoreCase = true)
        } == true
    if (!installed) throw BuiltinGeoAssetsRequiredException()
}
