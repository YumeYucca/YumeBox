package dev.yume.packer

import java.io.DataInputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.tukaani.xz.XZInputStream

fun verifyPackedPayload(apk: File) {
    ZipFile(apk).use { zip ->
        check(zip.getEntry("classes.dex") != null && zip.getEntry("classes2.dex") == null) { "Invalid loader DEX layout" }
        listOf("libloader", "libmihomo", "libpreview").forEach { check(zip.getEntry("lib/arm64-v8a/$it.so") != null) { "Missing bootstrap library $it" } }
        val metadata = requireNotNull(zip.getEntry(PayloadFormat.METADATA)) { "Missing loader metadata" }
        DataInputStream(zip.getInputStream(metadata)).use { input ->
            check(input.readInt() == PayloadFormat.MAGIC && input.readInt() == PayloadFormat.VERSION) { "Unsupported payload format" }
            check(input.readUTF().length == 64)
            check(input.readUTF() == "com.github.yumeyucca.yumebox.App")
            check(input.readUTF() == "androidx.core.app.CoreComponentFactory")
            fun verifyEntry(native: Boolean) {
                if (native) check(input.readUTF() == "arm64-v8a") { "Unexpected native ABI" }
                val path = input.readUTF()
                val name = input.readUTF()
                check(!name.contains('/') && !name.contains('\\') && path.startsWith("${PayloadFormat.ROOT}/"))
                val size = input.readLong()
                val expected = input.readNBytes(32)
                val entry = requireNotNull(zip.getEntry(path)) { "Missing payload $path" }
                val digest = MessageDigest.getInstance("SHA-256")
                var count = 0L
                XZInputStream(zip.getInputStream(entry)).use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        count += read
                        check(count <= size) { "Expanded payload exceeds declared length: $path" }
                        digest.update(buffer, 0, read)
                    }
                }
                check(count == size && digest.digest().contentEquals(expected)) { "Corrupt payload $path" }
            }
            val dexCount = input.readInt()
            check(dexCount in 1..zip.size())
            repeat(dexCount) { verifyEntry(false) }
            val nativeCount = input.readInt()
            check(nativeCount in 1..zip.size())
            repeat(nativeCount) { verifyEntry(true) }
            check(input.read() == -1) { "Trailing loader metadata" }
        }
    }
}
