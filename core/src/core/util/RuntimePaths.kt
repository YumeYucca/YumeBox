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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.core.util


import android.content.Context
import java.io.File

const val RUNTIME_HOME_DIR_NAME = "mihomo"
const val PROFILE_PROVIDERS_DIR_NAME = "providers"
const val RULE_PROVIDER_SCOPE = "rules"
const val PROXY_PROVIDER_SCOPE = "proxies"

// Kept as the literal "acg" so wallpapers copied under the old path stay resolvable after the rename.
const val MOE_ASSETS_DIR_NAME = "acg"
const val MOE_WALLPAPER_FILE_NAME = "wallpaper.dat"

val Context.runtimeHomeDir: File
    get() = filesDir.resolve(RUNTIME_HOME_DIR_NAME)

val Context.moeAssetsDir: File
    get() = filesDir.resolve(MOE_ASSETS_DIR_NAME)

/**
 * Stable app-private slot for the locally copied home wallpaper. The directory is created on access
 * so callers can stream-copy into the returned file directly.
 */
fun Context.moeWallpaperFile(): File =
    moeAssetsDir.apply { mkdirs() }.resolve(MOE_WALLPAPER_FILE_NAME)

fun profileProvidersRoot(profileDir: File): File = profileDir.resolve(PROFILE_PROVIDERS_DIR_NAME)

fun profileProviderScopeDir(profileDir: File, scope: String): File =
    profileProvidersRoot(profileDir).resolve(scope)
