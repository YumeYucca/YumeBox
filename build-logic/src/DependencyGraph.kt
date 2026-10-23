package dev.yume.build

import java.util.TreeMap

data class Coordinate(val group: String, val artifact: String, val version: String, val classifier: String = "")
data class RuntimeGraph(val included: List<Coordinate>, val excluded: Set<Coordinate>)

/** Pinned 0.13.0 tree adapter. Reconstruct the DAG before applying scoped exclusions. */
fun runtimeGraph(text: String): RuntimeGraph {
    val nodes = linkedSetOf<Coordinate>()
    val roots = linkedSetOf<Coordinate>()
    val edges = mutableMapOf<Coordinate, MutableSet<Coordinate>>()
    val stack = TreeMap<Int, Coordinate>()
    val coordinate = Regex("^([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([^\\s:()]+)(?::([^\\s:()]+))?(?: -> ([^\\s:()]+)(?::([^\\s:()]+))?)?(?: \\(.*\\))*$")
    text.lineSequence().forEach { line ->
        val offset = line.indexOfFirst { it.isLetterOrDigit() }
        if (offset < 0 || !line.take(offset).contains('─')) return@forEach
        stack.tailMap(offset, true).clear()
        val body = line.substring(offset)
        if (Regex("^[^:]+:main:").containsMatchIn(body) || body.endsWith("(c)")) return@forEach
        val match = coordinate.find(body) ?: return@forEach
        val version = match.groupValues[5].ifEmpty { match.groupValues[3] }
        if (version == "unspecified") return@forEach
        val node = Coordinate(match.groupValues[1], match.groupValues[2], version, match.groupValues[6].ifEmpty { match.groupValues[4] })
        nodes += node
        val parent = stack.lastEntry()?.value
        if (parent == null) roots += node else edges.getOrPut(parent) { linkedSetOf() } += node
        stack[offset] = node
    }
    require(nodes.isNotEmpty() && roots.isNotEmpty()) { "Empty or unsupported Kotlin Toolchain runtime graph" }
    val visited = mutableSetOf<Pair<Coordinate, Int>>()
    val included = linkedSetOf<Coordinate>()
    fun visit(node: Coordinate, inherited: Int) {
        val flags = inherited or when (node.group + ":" + node.artifact) {
            "com.android.tools.smali:smali-dexlib2" -> 1
            "io.github.rosemoe:language-textmate" -> 2
            else -> 0
        }
        if (!visited.add(node to flags)) return
        included += node
        edges[node].orEmpty().forEach { child ->
            if (flags and 1 != 0 && child.group == "com.google.guava" && child.artifact == "guava") return@forEach
            if (flags and 2 != 0 && child.group == "org.jruby.joni" && child.artifact == "joni") return@forEach
            visit(child, flags)
        }
    }
    roots.forEach { visit(it, 0) }
    included.groupBy { it.group + ":" + it.artifact + ":" + it.classifier }.forEach { (key, variants) ->
        require(variants.map { it.version }.distinct().size == 1) { "Ambiguous selected versions for $key: $variants" }
    }
    included.forEach { node ->
        val minimum = when {
            node.group.startsWith("tools.jackson") -> "3.1.8"
            node.group == "org.bouncycastle" -> "1.86"
            node.group == "io.netty" -> "4.1.136"
            node.group == "org.apache.httpcomponents" && node.artifact == "httpclient" -> "4.5.14"
            node.group == "org.apache.commons" && node.artifact == "commons-lang3" -> "3.20.0"
            else -> null
        }
        if (minimum != null) {
            val actual = node.version.split('.', '-').map { it.toIntOrNull() ?: 0 }
            val floor = minimum.split('.').map(String::toInt)
            val comparison = (0 until maxOf(actual.size, floor.size)).map { actual.getOrElse(it) { 0 }.compareTo(floor.getOrElse(it) { 0 }) }.firstOrNull { it != 0 } ?: 0
            require(comparison >= 0) { "Dependency below security floor: $node; pin at least $minimum in module.yaml" }
        }
    }
    return RuntimeGraph(included.sortedWith(compareBy(Coordinate::group, Coordinate::artifact, Coordinate::version)), nodes - included)
}
