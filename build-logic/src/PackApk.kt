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

package dev.yume.packer

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream

class ApkSigning(
    val keyStore: File,
    val keyAlias: String,
    val keyStorePassword: String,
    val keyPassword: String,
)

class ApkPacker(
    private val sdkDirectory: File,
    private val workingDirectory: File,
    private val loaderDex: File,
    private val originalApplication: String,
    private val originalComponentFactory: String,
    private val signing: ApkSigning? = null,
) {
    fun pack(input: File, output: File) {
        val work = workingDirectory.resolve(input.nameWithoutExtension)
        work.deleteRecursively()
        work.mkdirs()
        val unaligned = work.resolve("unaligned.apk")
        val aligned = work.resolve("aligned.apk")

        writePayload(input, unaligned)
        val tools = latestBuildTools()
        runCommand(
            listOf(
                tools.resolve(executable("zipalign")).absolutePath,
                "-P",
                "16",
                "-f",
                "4",
                unaligned.absolutePath,
                aligned.absolutePath,
            ),
            "zipalign failed for ${input.name}",
        )
        if (signing != null) sign(tools, aligned)
        aligned.copyTo(output, overwrite = true)
    }

    internal fun writePayload(input: File, output: File) {
        val dexPayload =
            ZipFile(input).use { zip ->
                zip.entries()
                    .asSequence()
                    .filter { !it.isDirectory && DEX_NAME.matches(it.name) }
                    .sortedBy(ZipEntry::getName)
                    .map { entry ->
                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        PayloadEntry(
                            assetName = "${PayloadFormat.ROOT}/dex/${entry.name}.xz",
                            outputName = entry.name,
                            originalSize = bytes.size.toLong(),
                            sha256 = sha256(bytes),
                            compressed = compress(bytes),
                        )
                    }
                    .toList()
            }
        check(dexPayload.isNotEmpty()) { "No classes*.dex found in $input" }
        val nativePayload =
            ZipFile(input).use { zip ->
                zip.entries()
                    .asSequence()
                    .filter { !it.isDirectory && NATIVE_LIBRARY.matches(it.name) }
                    .filterNot { BOOTSTRAP_LIBRARY.matches(it.name) }
                    .sortedBy(ZipEntry::getName)
                    .map { entry -> nativePayload(zip, entry) }
                    .toList()
            }
        check(nativePayload.distinctBy { it.abi to it.outputName }.size == nativePayload.size) {
            "Duplicate native library payload entries found in $input"
        }
        val payloadId = payloadId(dexPayload, nativePayload)
        val metadata = metadata(payloadId, dexPayload, nativePayload)

        ZipFile(input).use { source ->
            ZipOutputStream(output.outputStream().buffered()).use { target ->
                source
                    .entries()
                    .asSequence()
                    .filterNot { shouldDrop(it.name) }
                    .forEach { entry ->
                        if (entry.isDirectory) return@forEach
                        val bytes = source.getInputStream(entry).use { it.readBytes() }
                        target.put(bytes, entry.name, entry.method == ZipEntry.STORED)
                    }
                target.put(loaderDex.readBytes(), "classes.dex", stored = false)
                dexPayload.forEach { target.put(it.compressed, it.assetName, stored = true) }
                nativePayload.forEach { target.put(it.compressed, it.assetName, stored = true) }
                target.put(metadata, PayloadFormat.METADATA, stored = true)
            }
        }
    }

    private fun metadata(
        payloadId: String,
        dexEntries: List<PayloadEntry>,
        nativeEntries: List<NativePayloadEntry>,
    ): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(PayloadFormat.MAGIC)
                output.writeInt(PayloadFormat.VERSION)
                output.writeUTF(payloadId)
                output.writeUTF(originalApplication)
                output.writeUTF(originalComponentFactory)
                output.writeInt(dexEntries.size)
                dexEntries.forEach { entry ->
                    output.writeUTF(entry.assetName)
                    output.writeUTF(entry.outputName)
                    output.writeLong(entry.originalSize)
                    output.write(entry.sha256)
                }
                output.writeInt(nativeEntries.size)
                nativeEntries.forEach { entry ->
                    output.writeUTF(entry.abi)
                    output.writeUTF(entry.assetName)
                    output.writeUTF(entry.outputName)
                    output.writeLong(entry.originalSize)
                    output.write(entry.sha256)
                }
            }
            bytes.toByteArray()
        }

    private fun payloadId(
        dexEntries: List<PayloadEntry>,
        nativeEntries: List<NativePayloadEntry>,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        dexEntries.forEach { entry ->
            digest.update(entry.outputName.toByteArray(Charsets.UTF_8))
            digest.update(entry.sha256)
        }
        nativeEntries.forEach { entry ->
            digest.update(entry.abi.toByteArray(Charsets.UTF_8))
            digest.update(entry.outputName.toByteArray(Charsets.UTF_8))
            digest.update(entry.sha256)
        }
        return digest.digest().toHex()
    }

    private fun nativePayload(zip: ZipFile, entry: ZipEntry): NativePayloadEntry {
        val match =
            NATIVE_LIBRARY.matchEntire(entry.name) ?: error("Invalid native library: ${entry.name}")
        val abi = match.groupValues[1]
        val packagedName = match.groupValues[2]
        val packagedBytes = zip.getInputStream(entry).use { it.readBytes() }
        val alreadyCompressed = packagedName.endsWith(".xz.so")
        val originalBytes =
            if (alreadyCompressed) {
                org.tukaani.xz.XZInputStream(packagedBytes.inputStream()).use { it.readBytes() }
            } else {
                packagedBytes
            }
        val outputName =
            if (alreadyCompressed) {
                packagedName.removeSuffix(".xz.so") + ".so"
            } else {
                packagedName
            }
        return NativePayloadEntry(
            abi = abi,
            assetName = "${PayloadFormat.ROOT}/lib/$abi/$outputName.xz",
            outputName = outputName,
            originalSize = originalBytes.size.toLong(),
            sha256 = sha256(originalBytes),
            compressed = if (alreadyCompressed) packagedBytes else compress(originalBytes),
        )
    }

    private fun compress(bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().use { output ->
            XZOutputStream(output, LZMA2Options(6)).use { it.write(bytes) }
            output.toByteArray()
        }

    private fun shouldDrop(name: String): Boolean =
        DEX_NAME.matches(name) ||
            (NATIVE_LIBRARY.matches(name) && !BOOTSTRAP_LIBRARY.matches(name)) ||
            name.startsWith("${PayloadFormat.ROOT}/") ||
            SIGNATURE_ENTRY.matches(name)

    private fun sign(buildTools: File, apk: File) {
        val credentials = checkNotNull(signing) { "APK signing configuration is incomplete" }
        val signer = buildTools.resolve("lib/apksigner.jar")
        check(signer.isFile) { "apksigner.jar not found: $signer" }
        val command =
            listOf(
                javaExecutable(),
                "-jar",
                signer.absolutePath,
                "sign",
                "--ks",
                credentials.keyStore.absolutePath,
                "--ks-key-alias",
                credentials.keyAlias,
                "--ks-pass",
                "env:YUME_PACKER_KS_PASS",
                "--key-pass",
                "env:YUME_PACKER_KEY_PASS",
                apk.absolutePath,
            )
        val environment =
            mapOf(
                "YUME_PACKER_KS_PASS" to credentials.keyStorePassword,
                "YUME_PACKER_KEY_PASS" to credentials.keyPassword,
            )
        runCommand(command, "apksigner failed for ${apk.name}", environment)
    }

    private fun latestBuildTools(): File {
        val root = sdkDirectory.resolve("build-tools")
        return root.listFiles()?.filter(File::isDirectory)?.maxWithOrNull { left, right ->
            compareVersions(left.name, right.name)
        } ?: error("No Android build-tools installation found under $root")
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun executable(name: String): String =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "$name.exe"
        else name

    private fun javaExecutable(): String =
        File(System.getProperty("java.home"), "bin/java").absolutePath

    private fun runCommand(
        command: List<String>,
        failure: String,
        environment: Map<String, String> = emptyMap(),
    ) {
        val process =
            ProcessBuilder(command).apply { environment().putAll(environment) }.inheritIO().start()
        check(process.waitFor() == 0) { failure }
    }

    private fun ZipOutputStream.put(bytes: ByteArray, name: String, stored: Boolean) {
        val entry =
            ZipEntry(name).apply {
                time = FIXED_TIMESTAMP
                if (stored) {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = size
                    crc = CRC32().apply { update(bytes) }.value
                }
            }
        putNextEntry(entry)
        write(bytes)
        closeEntry()
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private data class PayloadEntry(
        val assetName: String,
        val outputName: String,
        val originalSize: Long,
        val sha256: ByteArray,
        val compressed: ByteArray,
    )

    private data class NativePayloadEntry(
        val abi: String,
        val assetName: String,
        val outputName: String,
        val originalSize: Long,
        val sha256: ByteArray,
        val compressed: ByteArray,
    )

    private companion object {
        val DEX_NAME = Regex("classes(?:[2-9][0-9]*)?\\.dex")
        val NATIVE_LIBRARY = Regex("lib/([^/]+)/([^/]+\\.so)")

        // Kept raw (not moved into the payload) so they stay under nativeLibraryDir:
        //  - libloader.so is the bootstrap loader itself;
        //  - libmihomo.so and libpreview.so are tiny PIE shells. A non-root app can only
        //    execve() files living in nativeLibraryDir; both dlopen the compressed core payload.
        val BOOTSTRAP_LIBRARY = Regex("lib/[^/]+/(?:libloader|libmihomo|libpreview)\\.so")
        val SIGNATURE_ENTRY =
            Regex("META-INF/[^/]+\\.(?:MF|SF|RSA|DSA|EC)", RegexOption.IGNORE_CASE)
        const val FIXED_TIMESTAMP = 315_532_800_000L
    }
}
