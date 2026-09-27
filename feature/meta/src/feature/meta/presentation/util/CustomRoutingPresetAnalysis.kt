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

package com.github.yumeyucca.yumebox.feature.meta.presentation.util

import com.github.yumeyucca.yumebox.core.util.YamlCodec

fun analyzePresetTemplateContent(content: String?): OverridePresetTemplateContentAnalysis {
    if (content.isNullOrBlank()) {
        return OverridePresetTemplateContentAnalysis(
            selection = defaultOverridePresetTemplateSelection(),
            matchesTemplateExactly = true,
        )
    }

    val document =
        runCatching { YamlCodec.loadValue(content).asStringKeyedMap() }
            .getOrElse {
                return OverridePresetTemplateContentAnalysis(
                    selection = defaultOverridePresetTemplateSelection(),
                    matchesTemplateExactly = false,
                )
            }
            ?: return OverridePresetTemplateContentAnalysis(
                selection = defaultOverridePresetTemplateSelection(),
                matchesTemplateExactly = false,
            )

    val selection =
        inferPresetTemplateSelection(document)
            ?: return OverridePresetTemplateContentAnalysis(
                selection = defaultOverridePresetTemplateSelection(),
                matchesTemplateExactly = false,
            )

    val generatedDocument = runCatching {
        buildPresetTemplateYaml(selection).let(YamlCodec::loadValue).asStringKeyedMap()
    }
        .getOrNull()

    return OverridePresetTemplateContentAnalysis(
        selection = selection,
        matchesTemplateExactly = generatedDocument != null && document == generatedDocument,
    )
}

fun inferPresetTemplateSelection(content: String?): OverridePresetTemplateSelection =
    analyzePresetTemplateContent(content).selection

private fun inferPresetTemplateSelection(
    document: Map<String, Any?>
): OverridePresetTemplateSelection? {
    val providerKeys = document.stringKeyedMap("rule-providers").keys
    val groupNames =
        document
            .listOfMaps("proxy-groups")
            .mapNotNull { group -> group["name"]?.toString()?.takeIf(String::isNotBlank) }
            .toSet()
    val rules = document.stringList("rules")

    val hasTemplateSignals =
        providerKeys.any(templateProviderIds::contains) ||
                groupNames.any(serviceGroupNames::contains) ||
                groupNames.any(regionGroupNames::contains) ||
                rules.any(::isOfficialMrsTemplateRule)

    if (!hasTemplateSignals) {
        return null
    }

    val inferredUrlTestRegions =
        orderedRegions.filter { it.groupName in groupNames }.toCollection(linkedSetOf())
    val inferredFallbackRegions =
        orderedRegions.filter { it.fallbackGroupName in groupNames }.toCollection(linkedSetOf())
    val inferredEnabledItems =
        orderedItems
            .filter { item ->
                isOfficialMrsItemEnabledInConfig(
                    item = item,
                    providerKeys = providerKeys,
                    groupNames = groupNames,
                    rules = rules,
                )
            }
            .toCollection(linkedSetOf())
            .ifEmpty { defaultEnabledPresetItems() }

    return OverridePresetTemplateSelection(
        urlTestRegions = inferredUrlTestRegions,
        fallbackRegions = inferredFallbackRegions,
        enabledItems = inferredEnabledItems,
        enableUrlTestGroup =
            OfficialMrs.autoGroupName in groupNames ||
                    orderedRegions.any { it.groupName in groupNames },
        enableFallbackGroup =
            OfficialMrs.fallbackGroupName in groupNames ||
                    orderedRegions.any { it.fallbackGroupName in groupNames },
    )
}

private fun isOfficialMrsItemEnabledInConfig(
    item: OverridePresetItem,
    providerKeys: Set<String>,
    groupNames: Set<String>,
    rules: List<String>,
): Boolean {
    if (item == OverridePresetItem.Match) {
        return rules.any { rule -> rule.trim() == OfficialMrs.matchRule }
    }
    val providerIds = item.providers.map(OfficialMrsProviderSpec::id)
    return providerIds.any(providerKeys::contains) ||
            item.detectionRules.any(rules::contains) ||
            (item.groupName != null && item.groupName in groupNames)
}

private fun isOfficialMrsTemplateRule(rule: String): Boolean {
    val normalizedRule = rule.trim()
    return normalizedRule in templateRules ||
            templateProviderIds.any { providerId -> normalizedRule.contains(providerId) }
}

private fun Map<String, Any?>.stringKeyedMap(key: String): Map<String, Any?> =
    (this[key] as? Map<*, *>)
        ?.entries
        ?.associate { entry -> entry.key.toString() to entry.value }
        .orEmpty()

private fun Map<String, Any?>.listOfMaps(key: String): List<Map<String, Any?>> =
    (this[key] as? List<*>)
        ?.mapNotNull { element ->
            (element as? Map<*, *>)?.entries?.associate { entry ->
                entry.key.toString() to entry.value
            }
        }
        .orEmpty()

private fun Map<String, Any?>.stringList(key: String): List<String> =
    (this[key] as? List<*>)
        ?.mapNotNull { value ->
            when (value) {
                null -> null
                is String -> value
                else -> value.toString()
            }
        }
        .orEmpty()

private fun Any?.asStringKeyedMap(): Map<String, Any?>? =
    (this as? Map<*, *>)?.entries?.associate { entry -> entry.key.toString() to entry.value }