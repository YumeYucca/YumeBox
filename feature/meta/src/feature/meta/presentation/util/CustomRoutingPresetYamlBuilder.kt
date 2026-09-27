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
                        enableUrlTestGroup = selection.enableUrlTestGroup,
                        enableFallbackGroup = selection.enableFallbackGroup,
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
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<Map<String, Any?>> {
    val regionNames = buildList {
        if (enableUrlTestGroup) {
            addAll(selectedUrlTestRegions.map(OverridePresetRegion::groupName))
        }
        if (enableFallbackGroup) {
            addAll(selectedFallbackRegions.map(OverridePresetRegion::fallbackGroupName))
        }
    }

    return buildList {
        add(
            buildProxySelectGroup(
                regionNames = regionNames,
                enableUrlTestGroup = enableUrlTestGroup,
                enableFallbackGroup = enableFallbackGroup,
            )
        )
        if (enableUrlTestGroup) {
            add(
                buildHealthCheckGroup(
                    name = OfficialMrs.autoGroupName,
                    type = OfficialMrsHealthCheckGroupType.UrlTest,
                )
            )
        }
        if (enableFallbackGroup) {
            add(
                buildHealthCheckGroup(
                    name = OfficialMrs.fallbackGroupName,
                    type = OfficialMrsHealthCheckGroupType.Fallback,
                )
            )
        }
        addAll(
            orderedServiceItems
                .filter { it in enabledItems }
                .map { item ->
                    buildServiceSelectGroup(
                        item = item,
                        regionNames = regionNames,
                        enableUrlTestGroup = enableUrlTestGroup,
                        enableFallbackGroup = enableFallbackGroup,
                    )
                }
        )
        if (enableUrlTestGroup) {
            addAll(
                selectedUrlTestRegions.map { region ->
                    buildHealthCheckGroup(
                        name = region.groupName,
                        type = OfficialMrsHealthCheckGroupType.UrlTest,
                        filter = region.filter,
                        excludeFilter = region.excludeFilter,
                        icon = region.icon,
                    )
                }
            )
        }
        if (enableFallbackGroup) {
            addAll(
                selectedFallbackRegions.map { region ->
                    buildHealthCheckGroup(
                        name = region.fallbackGroupName,
                        type = OfficialMrsHealthCheckGroupType.Fallback,
                        filter = region.filter,
                        excludeFilter = region.excludeFilter,
                        icon = region.icon,
                    )
                }
            )
        }
    }
}

private fun buildRules(enabledItems: Set<OverridePresetItem>): List<String> {
    val enabledItemIds = enabledItems.map(OverridePresetItem::id).toSet()
    return buildList {
        ruleOrder
            .filter { it.id in enabledItemIds }
            .forEach { item -> addAll(item.detectionRules) }
        if (OverridePresetItem.Match.id in enabledItemIds || isEmpty()) {
            if (OfficialMrs.matchRule !in this) {
                add(OfficialMrs.matchRule)
            }
        }
    }
}

private fun buildHealthCheckGroup(
    name: String,
    type: OfficialMrsHealthCheckGroupType,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String =
        when (type) {
            OfficialMrsHealthCheckGroupType.UrlTest ->
                OfficialMrs.catalogIconUrl("Urltest").orEmpty()

            OfficialMrsHealthCheckGroupType.Fallback ->
                OfficialMrs.catalogIconUrl("Available").orEmpty()
        },
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
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): Map<String, Any?> =
    linkedMapOf(
        "name" to OfficialMrs.proxyPolicy,
        "type" to "select",
        "icon" to OverridePresetItem.Proxy.icon.orEmpty(),
        "proxies" to
                buildSelectableGroupNames(
                    regionNames = regionNames,
                    enableUrlTestGroup = enableUrlTestGroup,
                    enableFallbackGroup = enableFallbackGroup,
                ),
        "include-all" to true,
    )

private fun buildServiceSelectGroup(
    item: OverridePresetItem,
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
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
                if (enableUrlTestGroup) {
                    add(OfficialMrs.autoGroupName)
                }
                if (enableFallbackGroup) {
                    add(OfficialMrs.fallbackGroupName)
                }
                addAll(regionNames)
            }
                .distinct(),
        )
    }
}

private fun buildSelectableGroupNames(
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<String> = buildList {
    if (enableUrlTestGroup) {
        add(OfficialMrs.autoGroupName)
    }
    if (enableFallbackGroup) {
        add(OfficialMrs.fallbackGroupName)
    }
    addAll(regionNames)
}
    .distinct()

private fun combineOfficialMrsExcludeFilters(extraExcludeFilter: String?): String =
    listOf(OfficialMrs.nodeExcludeFilter, extraExcludeFilter).filterNotNull().joinToString("|")

private fun OfficialMrsProviderSpec.urlTemplate(): String =
    when (behavior) {
        OfficialMrsRuleBehavior.Domain -> OfficialMrs.geositeUrlTemplate
        OfficialMrsRuleBehavior.IpCidr -> OfficialMrs.geoipUrlTemplate
    }