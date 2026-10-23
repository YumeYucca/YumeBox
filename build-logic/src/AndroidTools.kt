package dev.yume.packer

import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class AndroidTools(val sdk: File) {
    val directory = sdk.resolve("build-tools/37.0.0").also {
        require(it.isDirectory) { "Android build-tools 37.0.0 missing under $sdk" }
    }
    val java = File(System.getProperty("java.home"), "bin/java").absolutePath
    val aapt2 = directory.resolve(executable("aapt2")).absolutePath

    fun run(command: List<String>, environment: Map<String, String> = emptyMap()) {
        val process = ProcessBuilder(command).apply { environment().putAll(environment) }.inheritIO().start()
        check(process.waitFor() == 0) { "Android tool failed: ${command.first()}" }
    }

    fun finish(input: File, output: File, signing: ApkSigning?) {
        run(listOf(directory.resolve(executable("zipalign")).path, "-P", "16", "-f", "4", input.path, output.path))
        if (signing != null) {
            run(listOf(java, "-jar", directory.resolve("lib/apksigner.jar").path, "sign",
                "--ks", signing.keyStore.path, "--ks-key-alias", signing.keyAlias,
                "--ks-pass", "env:YUME_PACKER_KS_PASS", "--key-pass", "env:YUME_PACKER_KEY_PASS", output.path),
                mapOf("YUME_PACKER_KS_PASS" to signing.keyStorePassword, "YUME_PACKER_KEY_PASS" to signing.keyPassword))
        }
    }

    fun verify(apk: File, signing: ApkSigning?) {
        run(listOf(directory.resolve(executable("zipalign")).path, "-c", "-P", "16", "4", apk.path))
        if (signing != null) {
            run(listOf(java, "-jar", directory.resolve("lib/apksigner.jar").path, "verify", "--verbose", apk.path))
            val process = ProcessBuilder(java, "-jar", directory.resolve("lib/apksigner.jar").path, "verify", "--print-certs", apk.path)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            check(process.waitFor() == 0)
            val actual = Regex("certificate SHA-256 digest: ([a-fA-F0-9]+)").findAll(output).map { it.groupValues[1].lowercase() }.toSet()
            val keyStore = KeyStore.getInstance(signing.keyStore, signing.keyStorePassword.toCharArray())
            val expected = MessageDigest.getInstance("SHA-256").digest(keyStore.getCertificate(signing.keyAlias).encoded).joinToString("") { "%02x".format(it) }
            check(actual == setOf(expected)) { "Final APK certificate does not match the requested signing identity" }
        }
    }

    private fun executable(name: String) = if (System.getProperty("os.name").startsWith("Windows")) "$name.exe" else name
}

fun rewriteApk(input: File, output: File, additions: Map<String, ByteArray> = emptyMap(), exclude: (String) -> Boolean = { false }) {
    output.parentFile.mkdirs()
    require(input.canonicalFile != output.canonicalFile) { "APK input and output must differ" }
    ZipFile(input).use { source ->
        ZipOutputStream(output.outputStream().buffered()).use { target ->
            source.entries().asSequence().filterNot { it.isDirectory || it.name in additions || exclude(it.name) }.forEach { entry ->
                target.writeEntry(entry.name, source.getInputStream(entry).use { it.readBytes() }, entry.method == ZipEntry.STORED)
            }
            additions.toSortedMap().forEach { (name, bytes) ->
                require(!name.startsWith('/') && name.split('/').none { it == ".." }) { "Unsafe APK entry: $name" }
                target.writeEntry(name, bytes, name.endsWith(".so") || name.endsWith(".xz"))
            }
        }
    }
}

private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray, stored: Boolean) {
    putNextEntry(ZipEntry(name).apply {
        time = 315_532_800_000L
        if (stored) {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = size
            crc = CRC32().apply { update(bytes) }.value
        }
    })
    write(bytes)
    closeEntry()
}
