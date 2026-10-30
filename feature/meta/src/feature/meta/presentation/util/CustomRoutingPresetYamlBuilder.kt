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

fun buildPresetTemplateYaml(selection: OverridePresetTemplateSelection): String {
    val normalizedEnabledItems = normalizeEnabledItems(selection.enabledItems)
    val selectedUrlTestRegions = orderedRegions.filter { it in selection.urlTestRegions }
    val selectedFallbackRegions = orderedRegions.filter { it in selection.fallbackRegions }
    val document =
        linkedMapOf<String, Any?>(
            "rule-providers" to buildRuleProviders(normalizedEnabledItems),
            "proxy-groups" to
                    buildProxyGroups(
                        selectedUrlTestRegions = selectedUrlTestRegions,
                        selectedFallbackRegions = selectedFallbackRegions,
                        enabledItems = normalizedEnabledItems,
                        healthCheckGroups = selection.healthCheckGroups,
                    ),
            "rules" to buildRules(normalizedEnabledItems),
        )
            .filterValues { value ->
                when (value) {
                    is Collection<*> -> value.isNotEmpty()
                    is Map<*, *> -> value.isNotEmpty()
                    else -> value != null
                }
            }

    val yamlContent = YamlCodec.dumpMap(document)
    runCatching {
        YamlCodec.validate(yamlContent)
        YamlCodec.loadMap(yamlContent)
    }
        .getOrElse { error ->
            throw IllegalStateException(
                "Custom routing YAML self-check failed: ${error.message}",
                error,
            )
        }
    return yamlContent
}

private fun normalizeEnabledItems(items: Set<OverridePresetItem>): Set<OverridePresetItem> =
    items.ifEmpty {
        linkedSetOf(OverridePresetItem.Match)
    }

private fun buildRuleProviders(
    enabledItems: Set<OverridePresetItem>
): Map<String, Map<String, Any?>> =
    linkedMapOf<String, Map<String, Any?>>().apply {
        orderedItems
            .filter { item -> item in enabledItems && item != OverridePresetItem.Match }
            .flatMap(OverridePresetItem::providers)
            .forEach { provider ->
                put(
                    provider.id,
                    linkedMapOf(
                        "type" to "http",
                        "format" to "mrs",
                        "behavior" to provider.behavior.wireName,
                        "url" to provider.urlTemplate().format(provider.remoteName),
                        "path" to OfficialMrs.ruleProviderPath(provider.id),
                        "interval" to OfficialMrs.ruleProviderIntervalSeconds,
                    ),
                )
            }
    }

private fun buildProxyGroups(
    selectedUrlTestRegions: List<OverridePresetRegion>,
    selectedFallbackRegions: List<OverridePresetRegion>,
    enabledItems: Set<OverridePresetItem>,
    healthCheckGroups: Set<OfficialMrsHealthCheckGroupType>,
): List<Map<String, Any?>> {
    // Each health-check kind maps to one region list and one group-name accessor; pairing them once
    // keeps the selectable names and the emitted region groups in sync.
    val regionsByType =
        healthCheckGroups.ordered.map { type ->
            type to regionsFor(type, selectedUrlTestRegions, selectedFallbackRegions)
        }
    val regionNames =
        regionsByType.flatMap { (type, regions) -> regions.map { it.groupNameFor(type) } }

    return buildList {
        add(
            buildProxySelectGroup(
                regionNames = regionNames,
                healthCheckGroups = healthCheckGroups,
            )
        )
        healthCheckGroups.ordered.forEach { type ->
            add(buildHealthCheckGroup(name = type.groupName, type = type))
        }
        addAll(
            orderedServiceItems
                .filter { it in enabledItems }
                .map { item ->
                    buildServiceSelectGroup(
                        item = item,
                        regionNames = regionNames,
                        healthCheckGroups = healthCheckGroups,
                    )
                }
        )
        regionsByType.forEach { (type, regions) ->
            addAll(buildRegionHealthCheckGroups(regions = regions, type = type))
        }
    }
}

private fun buildRegionHealthCheckGroups(
    regions: List<OverridePresetRegion>,
    type: OfficialMrsHealthCheckGroupType,
): List<Map<String, Any?>> =
    regions.map { region ->
        buildHealthCheckGroup(
            name = region.groupNameFor(type),
            type = type,
            filter = region.filter,
            excludeFilter = region.excludeFilter,
            icon = region.icon,
        )
    }

private fun regionsFor(
    type: OfficialMrsHealthCheckGroupType,
    urlTestRegions: List<OverridePresetRegion>,
    fallbackRegions: List<OverridePresetRegion>,
): List<OverridePresetRegion> =
    when (type) {
        OfficialMrsHealthCheckGroupType.UrlTest -> urlTestRegions
        OfficialMrsHealthCheckGroupType.Fallback -> fallbackRegions
    }

private fun OverridePresetRegion.groupNameFor(type: OfficialMrsHealthCheckGroupType): String =
    when (type) {
        OfficialMrsHealthCheckGroupType.UrlTest -> groupName
        OfficialMrsHealthCheckGroupType.Fallback -> fallbackGroupName
    }

private fun buildRules(enabledItems: Set<OverridePresetItem>): List<String> =
    buildList {
        ruleOrder
            .filter { it in enabledItems }
            .forEach { item -> addAll(item.detectionRules) }
        // normalizeEnabledItems() keeps Match enabled when nothing else is and every item carries at
        // least one rule, so the list is non-empty here; the empty guard is kept as a cheap invariant.
        if (OverridePresetItem.Match in enabledItems || isEmpty()) {
            if (OfficialMrs.matchRule !in this) {
                add(OfficialMrs.matchRule)
            }
        }
    }

private fun buildHealthCheckGroup(
    name: String,
    type: OfficialMrsHealthCheckGroupType,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String = type.icon,
): Map<String, Any?> =
    linkedMapOf<String, Any?>().apply {
        put("name", name)
        put("type", type.wireName)
        put("icon", icon)
        put("url", OfficialMrs.urlTestUrl)
        put("interval", OfficialMrs.urlTestIntervalSeconds)
        put("include-all", true)
        put("exclude-filter", combineOfficialMrsExcludeFilters(excludeFilter))
        filter?.let { put("filter", it) }
    }

private fun buildProxySelectGroup(
    regionNames: List<String>,
    healthCheckGroups: Set<OfficialMrsHealthCheckGroupType>,
): Map<String, Any?> =
    linkedMapOf(
        "name" to OfficialMrs.proxyPolicy,
        "type" to "select",
        "icon" to OverridePresetItem.Proxy.icon.orEmpty(),
        "proxies" to
                buildSelectableGroupNames(
                    regionNames = regionNames,
                    healthCheckGroups = healthCheckGroups,
                ),
        "include-all" to true,
    )

private fun buildServiceSelectGroup(
    item: OverridePresetItem,
    regionNames: List<String>,
    healthCheckGroups: Set<OfficialMrsHealthCheckGroupType>,
): Map<String, Any?> {
    val serviceGroupName = checkNotNull(item.groupName)
    return linkedMapOf<String, Any?>().apply {
        put("name", serviceGroupName)
        put("type", "select")
        item.icon?.takeIf(String::isNotBlank)?.let { put("icon", it) }
        put(
            "proxies",
            buildList {
                add(OfficialMrs.proxyPolicy)
                add(OfficialMrs.directPolicy)
                addAll(healthCheckGroups.ordered.map(OfficialMrsHealthCheckGroupType::groupName))
                addAll(regionNames)
            }
                .distinct(),
        )
    }
}

private fun buildSelectableGroupNames(
    regionNames: List<String>,
    healthCheckGroups: Set<OfficialMrsHealthCheckGroupType>,
): List<String> =
    (healthCheckGroups.ordered.map(OfficialMrsHealthCheckGroupType::groupName) + regionNames)
        .distinct()

private val Set<OfficialMrsHealthCheckGroupType>.ordered: List<OfficialMrsHealthCheckGroupType>
    get() = OfficialMrsHealthCheckGroupType.entries.filter { it in this }

private fun combineOfficialMrsExcludeFilters(extraExcludeFilter: String?): String =
    listOf(OfficialMrs.nodeExcludeFilter, extraExcludeFilter).filterNotNull().joinToString("|")

private fun OfficialMrsProviderSpec.urlTemplate(): String =
    when (behavior) {
        OfficialMrsRuleBehavior.Domain -> OfficialMrs.geositeUrlTemplate
        OfficialMrsRuleBehavior.IpCidr -> OfficialMrs.geoipUrlTemplate
    }
