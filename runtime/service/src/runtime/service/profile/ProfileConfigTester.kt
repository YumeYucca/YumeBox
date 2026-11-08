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

package com.github.yumeyucca.yumebox.runtime.service.profile

import android.annotation.SuppressLint
import android.content.Context
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.runtime.service.core.CoreArtifacts
import java.io.File
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Validates a profile's main `config.yaml` with the packaged mihomo shell (`libmihomo.so --test`), without applying
 * overrides or runtime patches. Used on first import so a broken main config is rejected before commit.
 *
 * This test is the gate that decides whether a subscription is importable at all.
 */
internal object ProfileConfigTester {
    private const val TAG = "ProfileConfigTester"
    private const val TIMEOUT_MS = 20_000L
    private const val MAX_ERROR_CHARS = 400
    private const val CORE_FATAL_PREFIX = "mihomo:"
    private const val TEST_FAILED_MARKER = "test failed:"

    fun validateMainConfigOrThrow(context: Context, configFile: File) {
        validateMainConfig(context, configFile).getOrThrow()
    }

    fun validateMainConfig(
        context: Context,
        configFile: File,
    ): Result<Unit> {
        if (!configFile.isFile || configFile.length() <= 0L) {
            return reject(IllegalArgumentException("config.yaml is missing or empty"))
        }
        if (isAgeEncrypted(configFile)) {
            // Ciphertext cannot be parsed by mihomo; age-key handling stays on the existing path.
            Timber.tag(TAG).i("Skip mihomo test for age-encrypted profile config")
            return Result.success(Unit)
        }

        val binary =
            resolveCoreBinary(context)
                ?: return reject(IllegalStateException("mihomo core binary unavailable for config test"))
        // Match runtime: GEOIP/GEOSITE resolution keys off the core home, not the staging cwd.
        // Desktop `mihomo -t` always has a home; without --home the parser builds an empty MMDB
        // path and false-fails valid subscriptions on rules like GEOIP,CN.
        val homeDir = context.runtimeHomeDir.apply { mkdirs() }
        return try {
            val process =
                ProcessBuilder(
                        binary.absolutePath,
                        CoreArtifacts.LIBRARY_OPTION,
                        CoreArtifacts.library(context).absolutePath,
                        "--test",
                        "--home",
                        homeDir.absolutePath,
                        "--config",
                        configFile.absolutePath,
                    )
                    .directory(homeDir)
                    .redirectErrorStream(true)
                    .also { builder ->
                        val nativeLibDir = context.applicationInfo.nativeLibraryDir
                        val env = builder.environment()
                        val existing = env["LD_LIBRARY_PATH"].orEmpty()
                        env["LD_LIBRARY_PATH"] = if (existing.isBlank()) nativeLibDir else "$nativeLibDir:$existing"
                    }
                    .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return reject(IllegalStateException("mihomo config test timed out after ${TIMEOUT_MS}ms"))
            }
            if (process.exitValue() == 0) {
                Result.success(Unit)
            } else {
                reject(IllegalArgumentException(readableError(output)))
            }
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "mihomo config test failed to run")
            reject(
                IllegalStateException(
                    error.message?.takeIf { it.isNotBlank() } ?: "mihomo config test failed to run",
                    error,
                )
            )
        }
    }

    private fun reject(error: Throwable): Result<Unit> = Result.failure(error)

    private fun isAgeEncrypted(configFile: File): Boolean {
        val header =
            configFile.inputStream().use { input ->
                val buffer = ByteArray(48)
                val read = input.read(buffer)
                if (read <= 0) "" else buffer.decodeToString(0, read)
            }
        return header.startsWith("age-encryption.org/v1") || header.startsWith("-----BEGIN AGE ENCRYPTED FILE-----")
    }

    private fun resolveCoreBinary(context: Context): File? {
        val bundled = CoreArtifacts.shell(context)
        if (bundled.isFile && bundled.canExecute()) {
            return bundled
        }
        return runCatching { extractBin(context) }.getOrNull()
    }

    @SuppressLint("SetWorldReadable")
    private fun extractBin(context: Context): File {
        val src = CoreArtifacts.shell(context)
        val dst = File(context.filesDir, "bin/mihomo")
        dst.parentFile?.mkdirs()
        if (!dst.exists() || dst.length() != src.length()) {
            src.copyTo(dst, overwrite = true)
        }
        dst.setReadable(true, false)
        dst.setExecutable(true, false)
        return dst
    }

    /**
     * The condensed, user-facing reason. The native entry point reports a failed parse as `mihomo: configuration file
     * <path> test failed: <cause>`; only `<cause>` is actionable, the rest is an app-private path.
     */
    private fun readableError(output: String): String {
        val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val fatal = lines.lastOrNull { it.startsWith(CORE_FATAL_PREFIX) }?.removePrefix(CORE_FATAL_PREFIX)?.trim()
        val cause = fatal?.let { line ->
            val marker = line.indexOf(TEST_FAILED_MARKER, ignoreCase = true)
            if (marker < 0) line else line.substring(marker + TEST_FAILED_MARKER.length).trim()
        }
        val message =
            cause?.takeIf(String::isNotBlank)
                ?: lines.lastOrNull { it.contains("error", ignoreCase = true) }
                ?: lines.lastOrNull()
                ?: "configuration test failed"
        return message.take(MAX_ERROR_CHARS)
    }
}
