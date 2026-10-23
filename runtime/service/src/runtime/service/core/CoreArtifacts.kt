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

import android.content.Context
import java.io.File

/** Resolves the APK shell and the selected downloaded core, falling back to bundled Alpha. */
internal object CoreArtifacts {
    const val SHELL_NAME = "libmihomo.so"
    const val PREVIEW_SHELL_NAME = "libpreview.so"
    const val LIBRARY_NAME = "libmihomocore.so"
    const val LIBRARY_OPTION = "--core-library"

    fun shell(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, SHELL_NAME)

    fun previewShell(context: Context): File =
        File(context.applicationInfo.nativeLibraryDir, PREVIEW_SHELL_NAME)

    fun library(context: Context): File {
        KernelManager.installed(context)?.let { return it }
        return bundledLibrary(context)
    }

    /** Preview needs the bundled core that implements the private `--mode preview` protocol. */
    fun previewLibrary(context: Context): File = bundledLibrary(context)

    private fun bundledLibrary(context: Context): File {
        // Keep the untransformed debug APK path working. Release APKs normally move this file
        // into the loader payload, but Android Gradle's ordinary debug packaging may leave it in
        // nativeLibraryDir.
        File(context.applicationInfo.nativeLibraryDir, LIBRARY_NAME)
            .takeIf(File::isFile)
            ?.let { return it }

        // Alpha remains bundled as the first-run/offline baseline. The payload loader extracts it
        // to app-private storage; remote kernels use the same directory but always take priority.
        return runCatching {
                val bridge =
                    Class.forName("com.github.yumeyucca.yumebox.loader.PayloadRuntime", false, context.classLoader)
                bridge
                    .getMethod("findNativeLibrary", String::class.java)
                    .invoke(null, LIBRARY_NAME) as? String
            }
            .getOrNull()
            ?.let(::File)
            ?.takeIf(File::isFile)
            ?: error("Bundled Alpha mihomo core is unavailable")
    }

    fun arguments(context: Context, coreArgs: Array<String>): Array<String> =
        arrayOf(LIBRARY_OPTION, library(context).absolutePath, *coreArgs)

    fun previewArguments(context: Context, coreArgs: Array<String>): Array<String> =
        arrayOf(LIBRARY_OPTION, previewLibrary(context).absolutePath, *coreArgs)
}
