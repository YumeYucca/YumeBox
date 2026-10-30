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

package com.github.yumeyucca.yumebox.core.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import java.util.regex.Pattern

object YamlCodec {
    private fun newYaml(): Yaml {
        val options =
            DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
                isPrettyFlow = true
                indent = 2
                indicatorIndent = 0
                width = 160
                splitLines = false
            }
        return Yaml(SafeConstructor(), Representer(), options, JsonBooleanResolver())
    }

    fun <T> encode(serializer: KSerializer<T>, value: T): String {
        val element = PrettyPersistedJson.encodeToJsonElement(serializer, value)
        val tree = toYamlNode(element)
        return dumpValue(tree)
    }

    fun <T> decode(serializer: KSerializer<T>, content: String): T {
        val loaded = loadValue(content)
        val element = toJsonElement(loaded)
        return PrettyPersistedJson.decodeFromJsonElement(serializer, element)
    }

    fun dumpMap(value: Map<String, Any?>): String = dumpValue(value)

    @Suppress("UNCHECKED_CAST")
    fun loadMap(content: String): Map<String, Any?> {
        if (content.isBlank()) return emptyMap()
        val loaded = loadValue(content)
        require(loaded is Map<*, *>) { "YAML document root must be a map" }
        return loaded as Map<String, Any?>
    }

    fun dumpValue(value: Any?): String = newYaml().dump(normalizeYamlValue(value))

    fun loadValue(content: String): Any? = normalizeYamlValue(newYaml().load(content))

    fun validate(content: String) {
        if (content.isBlank()) return
        newYaml().load(content)
    }

    private fun toYamlNode(element: JsonElement): Any? =
        when (element) {
            JsonNull -> null
            is JsonObject ->
                LinkedHashMap<String, Any?>().apply {
                    element.forEach { (key, value) -> put(key, toYamlNode(value)) }
                }

            is JsonArray -> element.map(::toYamlNode)
            is JsonPrimitive ->
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.booleanOrNull
                    element.intOrNull != null -> element.intOrNull
                    element.longOrNull != null -> element.longOrNull
                    element.doubleOrNull != null -> element.doubleOrNull
                    else -> element.content
                }
        }

    private fun toJsonElement(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is Map<*, *> ->
                JsonObject(
                    LinkedHashMap<String, JsonElement>().apply {
                        value.forEach { (key, child) -> put(key.toString(), toJsonElement(child)) }
                    }
                )

            is List<*> -> JsonArray(value.map(::toJsonElement))
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value.toDouble())
            else -> JsonPrimitive(value.toString())
        }

    private fun normalizeYamlValue(value: Any?): Any? =
        when (value) {
            null -> null
            is Map<*, *> ->
                LinkedHashMap<String, Any?>().apply {
                    value.forEach { (key, childValue) ->
                        put(key.toString(), normalizeYamlValue(childValue))
                    }
                }

            is Iterable<*> -> value.map(::normalizeYamlValue)
            is Array<*> -> value.map(::normalizeYamlValue)
            else -> value
        }

    private class JsonBooleanResolver : Resolver() {
        override fun addImplicitResolvers() {
            addImplicitResolver(Tag.BOOL, JSON_BOOLEAN, "tTfF")
            addImplicitResolver(Tag.INT, INT, "-+0123456789")
            addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.")
            addImplicitResolver(Tag.MERGE, MERGE, "<")
            addImplicitResolver(Tag.NULL, NULL, "~nN\u0000")
            addImplicitResolver(Tag.NULL, EMPTY, null)
            addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789")
            addImplicitResolver(Tag.YAML, YAML, "!&*")
        }

        private companion object {
            val JSON_BOOLEAN: Pattern = Pattern.compile("^(?:true|True|TRUE|false|False|FALSE)$")
        }
    }
}
