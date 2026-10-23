package dev.yume.build

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.json.*
import org.w3c.dom.Element

// JitPack-published artifacts ship bare POMs without <licenses>; attribution was
// audited against the upstream repositories and must stay explicit here.
private val supplementalLicenses = mapOf(
    "com.github.topjohnwu.libsu:core" to listOf("Apache-2.0"),
    "com.github.topjohnwu.libsu:service" to listOf("Apache-2.0"),
)

fun collectLicenses(root: File, coordinates: List<Coordinate>, maven: MavenFiles): ByteArray {
    val licenseCatalog = Json.parseToJsonElement(root.resolve("build-logic/licenses.json").readText()).jsonObject
    val pomCache = ConcurrentHashMap<Coordinate, Element>()
    fun pom(coordinate: Coordinate): Element = pomCache.computeIfAbsent(coordinate) {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        factory.newDocumentBuilder().parse(maven.artifact(it.group, it.artifact, it.version, "pom")).documentElement
    }
    fun licenseElements(element: Element, depth: Int = 0): List<Element> {
        require(depth < 12) { "Maven parent cycle in license metadata" }
        val own = element.child("licenses")?.children("license").orEmpty()
        if (own.isNotEmpty()) return own
        val parent = element.child("parent") ?: return emptyList()
        return licenseElements(pom(Coordinate(parent.text("groupId"), parent.text("artifactId"), parent.text("version"))), depth + 1)
    }
    fun licenseId(element: Element): String {
        val name = element.text("name")
        val url = element.text("url")
        val normalized = "$name $url".lowercase()
        val known = when {
            normalized.contains("apache") -> "Apache-2.0"
            normalized.contains("ml kit") || normalized.contains("ml-kit/terms") -> "9962f44671a86e167c028415c7e21548"
            normalized.contains("lgpl") && normalized.contains("2.1") -> "36c8ce2eb10d65ef5edf4a5fdd06186e"
            normalized.contains("android software development kit") || normalized.contains("android.com/studio/terms") -> "ASDKL"
            normalized.contains("0bsd") || normalized.contains("zero clause") -> "0BSD"
            normalized.contains("bsd") && (normalized.contains("3") || normalized.contains("new") || normalized.contains("revised")) -> "BSD-3-Clause"
            normalized.contains("eclipse public") && normalized.contains("2.0") || Regex("\\bepl-?2\\.0\\b").containsMatchIn(normalized) -> "EPL-2.0"
            Regex("\\bmit\\b").containsMatchIn(normalized) -> "MIT"
            else -> licenseCatalog.entries.firstOrNull { (_, entry) -> entry.jsonObject["url"]?.jsonPrimitive?.content == url }?.key
        }
        return requireNotNull(known?.takeIf { it in licenseCatalog }) { "Unmapped license: $name ($url); add an audited license definition before packaging" }
    }
    val executor = Executors.newFixedThreadPool(8)
    val libraries = try {
        coordinates.map { coordinate -> executor.submit<JsonObject?> {
            val element = pom(coordinate)
            if (element.text("packaging") == "pom") return@submit null
            val ids = licenseElements(element).map(::licenseId).distinct()
                .ifEmpty { supplementalLicenses["${coordinate.group}:${coordinate.artifact}"].orEmpty() }
            require(ids.isNotEmpty()) { "Missing license metadata for $coordinate" }
            buildJsonObject {
                put("uniqueId", "${coordinate.group}:${coordinate.artifact}")
                put("artifactVersion", coordinate.version)
                put("name", element.text("name").ifBlank { coordinate.artifact })
                put("description", element.text("description"))
                element.text("url").takeIf { it.startsWith("http") }?.let { put("website", it) }
                element.child("organization")?.let { organization ->
                    putJsonObject("organization") {
                        put("name", organization.text("name"))
                        organization.text("url").takeIf(String::isNotBlank)?.let { put("url", it) }
                    }
                }
                element.child("scm")?.let { scm ->
                    putJsonObject("scm") {
                        listOf("url", "connection", "developerConnection").forEach { key ->
                            scm.text(key).takeIf(String::isNotBlank)?.let { put(key, it) }
                        }
                    }
                }
                putJsonArray("licenses") { ids.forEach { add(it) } }
                putJsonArray("developers") { element.child("developers")?.children("developer")?.forEach { developer -> add(buildJsonObject { put("name", developer.text("name")) }) } }
            }
        } }.mapNotNull { it.get() }
    } finally {
        executor.shutdownNow()
    }
    require(libraries.isNotEmpty()) { "No runtime library metadata was collected" }
    return buildJsonObject {
        put("libraries", JsonArray(libraries))
        put("licenses", licenseCatalog)
    }.toString().toByteArray(Charsets.UTF_8)
}

private fun Element.children(name: String): List<Element> = synchronized(ownerDocument ?: this) {
    (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }.filter { it.tagName == name }
}
private fun Element.child(name: String): Element? = children(name).firstOrNull()
private fun Element.text(name: String): String = synchronized(ownerDocument ?: this) {
    child(name)?.textContent?.trim().orEmpty()
}
