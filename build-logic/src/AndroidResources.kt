package dev.yume.build

import dev.yume.packer.AndroidTools
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlinx.serialization.json.*

data class ResourceAar(val file: File, val namespace: String, val module: String)

fun androidSdk(root: File): File {
    val default = when {
        System.getProperty("os.name").startsWith("Windows") -> File(System.getenv("LOCALAPPDATA"), "Android/Sdk")
        System.getProperty("os.name").startsWith("Mac") -> File(System.getProperty("user.home"), "Library/Android/sdk")
        else -> File(System.getProperty("user.home"), "Android/Sdk")
    }
    return listOfNotNull(System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT"), loadProperties(root.resolve("local.properties")).getProperty("sdk.dir"), default.path)
        .map(::File).firstOrNull(File::isDirectory) ?: error("Configure ANDROID_HOME with SDK37 and build-tools37.0.0")
}

/** Resource-only AARs preserve local library manifests/assets/namespaces missing from 0.13.0's AAR bridge. */
fun resourceAars(root: File, tools: AndroidTools): List<ResourceAar> {
    val modules = root.resolve("project.yaml").readLines().takeWhile { it != "plugins:" }
        .filter { it.startsWith("  - ") }.map { root.resolve(it.removePrefix("  - ")) }
    val androidJar = listOf("android-37.0", "android-37").map { tools.sdk.resolve("platforms/$it/android.jar") }.firstOrNull(File::isFile)
        ?: error("Android SDK37 platform missing")
    val headers = linkedMapOf<String, String>()
    val result = modules.filter { it.name !in setOf("app", "pack", "api", "packaging", "build-logic", "resource-symbols") }.mapNotNull { module ->
        val manifest = module.resolve("src/AndroidManifest.xml")
        val resources = module.resolve("res")
        val assets = module.resolve("assets")
        if (!manifest.isFile && !resources.isDirectory && !assets.isDirectory) return@mapNotNull null
        val namespace = Regex("(?m)^\\s+namespace:\\s*([^\\s#]+)").find(module.resolve("module.yaml").readText())?.groupValues?.get(1)
            ?: error("Missing resource namespace for $module")
        val inputs = listOf(manifest) + listOf(resources, assets).filter(File::isDirectory).flatMap { it.walkTopDown().filter(File::isFile).toList() }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("resource-symbols-v1".toByteArray())
        digest.update(namespace.toByteArray())
        inputs.filter(File::isFile).sortedBy { it.relativeTo(module).path }.forEach { file ->
            digest.update(file.relativeTo(module).invariantSeparatorsPath.toByteArray())
            digest.update(0)
            digest.update(file.readBytes())
        }
        val hash = digest.digest().take(12).joinToString("") { "%02x".format(it) }
        val work = root.resolve(".kotlin/yumebox/resources/${module.name}-$hash").apply { mkdirs() }
        val aar = work.resolve("resources.aar")
        if (!aar.isFile) {
            val factory = DocumentBuilderFactory.newInstance().apply { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            val document = if (manifest.isFile) factory.newDocumentBuilder().parse(manifest) else factory.newDocumentBuilder().newDocument().apply { appendChild(createElement("manifest")) }
            document.documentElement.setAttribute("package", namespace)
            val manifestBytes = ByteArrayOutputStream().also {
                TransformerFactory.newInstance().apply { setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "") }
                    .newTransformer().transform(DOMSource(document), StreamResult(it))
            }.toByteArray()
            val symbols = work.resolve("R.txt")
            if (resources.isDirectory) {
                val compiled = work.resolve("compiled.zip")
                tools.run(listOf(tools.aapt2, "compile", "--dir", resources.path, "-o", compiled.path))
                val stub = work.resolve("AndroidManifest.xml").apply { writeText("<manifest package=\"$namespace\"/>") }
                tools.run(listOf(tools.aapt2, "link", "--static-lib", "--non-final-ids", "--java", work.resolve("java").path, "--manifest", stub.path, "-I", androidJar.path, compiled.path, "--output-text-symbols", symbols.path, "-o", work.resolve("static.ap_").path))
            }
            ZipOutputStream(aar.outputStream().buffered()).use { zip ->
                fun add(name: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(name).apply { time = 315_532_800_000L })
                    zip.write(bytes); zip.closeEntry()
                }
                add("AndroidManifest.xml", manifestBytes)
                add("R.txt", if (symbols.isFile) symbols.readBytes() else byteArrayOf())
                add("classes.jar", ByteArrayOutputStream().also { JarOutputStream(it).close() }.toByteArray())
                listOf(resources, assets).filter(File::isDirectory).forEach { directory ->
                    directory.walkTopDown().filter(File::isFile).sortedBy(File::getPath).forEach { file -> add(directory.name + "/" + file.relativeTo(directory).invariantSeparatorsPath, file.readBytes()) }
                }
            }
        }
        work.resolve("java").takeIf(File::isDirectory)?.let { directory ->
            directory.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { headers[it.relativeTo(directory).invariantSeparatorsPath] = it.readText() }
        }
        ResourceAar(aar, namespace, module.name)
    }
    val serialized = buildJsonObject { headers.toSortedMap().forEach { (path, content) -> put(path, content) } }.toString()
    val index = root.resolve(".kotlin/yumebox/resource-symbols.json")
    if (!index.isFile || index.readText() != serialized) index.writeText(serialized)
    return result
}
