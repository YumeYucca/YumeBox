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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object OfficialMrs {
    val ruleProviderIntervalSeconds = 86_400
    val urlTestIntervalSeconds = 300
    val urlTestUrl = "https://www.gstatic.com/generate_204"
    val nodeExcludeFilter = "(?i)GB|Traffic|Expire|Premium|频道|订阅|ISP|流量|到期|重置"
    val geositeUrlTemplate =
        "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/%s.mrs"
    val geoipUrlTemplate =
        "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geoip/%s.mrs"
    val autoGroupName = "Auto"
    val fallbackGroupName = "Fallback"
    val proxyPolicy = "Proxy"
    val directPolicy = "DIRECT"
    val rejectPolicy = "REJECT"
    val matchRule = "MATCH,Proxy"

    private val catalogIconBaseUrl = "https://raw.githubusercontent.com/Orz-3/mini/master/Color"
    private val appIconBaseUrl =
        "https://raw.githubusercontent.com/fmz200/wool_scripts/main/icons/apps"

    fun ruleProviderPath(providerId: String): String = "./providers/rules/$providerId.mrs"

    fun catalogIconUrl(iconName: String?): String? = iconUrl(catalogIconBaseUrl, iconName, ".png")

    fun appIconUrl(iconName: String?): String? = iconUrl(appIconBaseUrl, iconName)

    private fun iconUrl(baseUrl: String, iconName: String?, suffix: String = ""): String? {
        val normalizedIconName = iconName?.trim()?.takeIf(String::isNotBlank) ?: return null
        return "$baseUrl/${encodePathSegment(normalizedIconName)}$suffix"
    }
}

private fun regionFilter(keywords: String): String = "(?i)($keywords)"

data class OverridePresetTemplateSelection(
    val urlTestRegions: Set<OverridePresetRegion> = emptySet(),
    val fallbackRegions: Set<OverridePresetRegion> = emptySet(),
    val enabledItems: Set<OverridePresetItem> = defaultEnabledPresetItems(),
    val enableUrlTestGroup: Boolean = true,
    val enableFallbackGroup: Boolean = false,
)

data class OverridePresetTemplateContentAnalysis(
    val selection: OverridePresetTemplateSelection,
    val matchesTemplateExactly: Boolean,
)

enum class OverridePresetRegion(definition: RegionDefinition) {
    HK(region("HK", "香港自动组", "香港|HK|Hong Kong|\\x{1F1ED}\\x{1F1F0}")),
    TW(region("TW", "台湾自动组", "台湾|TW|Taiwan|\\x{1F1F9}\\x{1F1FC}")),
    JP(region("JP", "日本自动组", "日本|JP|Japan|东京|大阪|\\x{1F1EF}\\x{1F1F5}")),
    SG(region("SG", "新加坡自动组", "新加坡|SG|Singapore|狮城|\\x{1F1F8}\\x{1F1EC}")),
    US(region("US", "美国自动组", "美国|US|United States|America|洛杉矶|硅谷|\\x{1F1FA}\\x{1F1F8}")),
    Other(
        region(
            label = "Other",
            displayName = "冷门地区自动组",
            specId = "other",
            iconName = "XD",
        )
    ),
    ;

    val specId: String = definition.specId
    val displayName: String = definition.displayName
    val groupName: String = definition.groupName
    val fallbackGroupName: String = definition.fallbackGroupName
    val filter: String? = definition.keywords?.let(::regionFilter)
    val excludeFilter: String?
        get() = if (this == Other) popularExclude else null
    val icon: String = OfficialMrs.catalogIconUrl(definition.iconName).orEmpty()
    private val keywords: String? = definition.keywords

    private companion object {
        val popularExclude: String by lazy {
            regionFilter(entries.mapNotNull { region -> region.keywords }.joinToString("|"))
        }
    }
}

enum class OverridePresetItem(definition: PresetItemDefinition) {
    Proxy(
        route(
            id = "proxy",
            title = "代理规则集",
            summary = "启用 proxy 规则集并走 ${OfficialMrs.proxyPolicy}.",
            icon = catalogIcon("Static"),
            sources = listOf(source(OfficialMrs.proxyPolicy, "proxy")),
        )
    ),
    Ads(
        route(
            id = "ads",
            title = "广告拦截",
            summary = "启用 category-ads-all 并走 ${OfficialMrs.rejectPolicy}.",
            icon = catalogIcon("Adblock"),
            sources = listOf(source(OfficialMrs.rejectPolicy, "category-ads-all")),
        )
    ),
    Google(service("google", "Google", catalogIcon("Google"), includeIp = true)),
    Telegram(service("telegram", "Telegram", catalogIcon("Telegram"), includeIp = true)),
    WhatsApp(service("whatsapp", "WhatsApp", appIcon("WhatsApp.png"))),
    Line(service("line", "LINE", catalogIcon("LineTV"), groupName = "LINE")),
    Twitter(service("twitter", "Twitter / X", catalogIcon("Twitter"), groupName = "Twitter", includeIp = true)),
    TikTok(service("tiktok", "TikTok", catalogIcon("TikTok"))),
    Speedtest(service("speedtest", "Speedtest", catalogIcon("Speedtest"))),
    GitHub(service("github", "GitHub", appIcon("github_00.png"))),
    Discord(service("discord", "Discord", appIcon("Discord.png"))),
    Reddit(service("reddit", "Reddit", appIcon("Category_Magazine.png"))),
    Facebook(service("facebook", "Facebook", appIcon("facebook.png"), includeIp = true)),
    Instagram(service("instagram", "Instagram", catalogIcon("Instagram"))),
    Threads(service("threads", "Threads", appIcon("Threads.png"))),
    Microsoft(service("microsoft", "Microsoft", catalogIcon("Microsoft"))),
    Bing(service("bing", "Bing", appIcon("bing.png"))),
    Apple(service("apple", "Apple", catalogIcon("Apple"))),
    YouTube(service("youtube", "YouTube", catalogIcon("YouTube"))),
    Netflix(service("netflix", "Netflix", catalogIcon("Netflix"))),
    Disney(service("disney", "Disney", catalogIcon("DisneyPlus"))),
    Hbo(service("hbo", "HBO", catalogIcon("HBO"))),
    PrimeVideo(service("primevideo", "Prime Video", catalogIcon("PrimeVideo"), groupName = "PrimeVideo")),
    Tvb(service("tvb", "TVB", appIcon("TVBAnywhere+.png"))),
    MyTvSuper(service("mytvsuper", "MyTVSuper", appIcon("TVBAnywhere+.png"))),
    Dazn(service("dazn", "DAZN", catalogIcon("Streaming"))),
    Spotify(service("spotify", "Spotify", catalogIcon("Spotify"))),
    Amazon(service("amazon", "Amazon", appIcon("Amazon.png"))),
    PayPal(service("paypal", "PayPal", catalogIcon("Paypal"))),
    Cloudflare(service("cloudflare", "Cloudflare", appIcon("Cloudflare.png"))),
    Zoom(service("zoom", "Zoom", appIcon("Category_Networking.png"))),
    Wikimedia(service("wikimedia", "Wikimedia", appIcon("Category_Research.png"))),
    Bilibili(service("bilibili", "Bilibili", catalogIcon("Bili"))),
    BiliIntl(service("biliintl", "BiliIntl", catalogIcon("Bili"))),
    Bahamut(service("bahamut", "Bahamut", catalogIcon("Bahamut"))),
    Dmm(service("dmm", "DMM", appIcon("AnimeHome.png"))),
    Abema(service("abema", "Abema", appIcon("AnimeHome.png"))),
    Ehentai(service("ehentai", "EHentai", appIcon("HentaiHome.png"))),
    OpenAI(service("openai", "OpenAI", appIcon("ChatGPT.png"))),
    Anthropic(
        service(
            "anthropic",
            "Claude / Anthropic",
            appIcon("Claude_01.png"),
            groupName = "Claude",
        )
    ),
    OneDrive(service("onedrive", "OneDrive", appIcon("OneDrive.png"))),
    Pixiv(service("pixiv", "Pixiv", appIcon("Category_Photo.png"))),
    Niconico(service("niconico", "Niconico", appIcon("AnimeHome.png"))),
    Steam(service("steam", "Steam", appIcon("steam.png"))),
    Cn(
        route(
            id = "cn",
            title = "中国大陆直连",
            summary = "启用 cn 域名和 IP 规则并走 ${OfficialMrs.directPolicy}.",
            icon = catalogIcon("China"),
            sources =
                listOf(
                    source(OfficialMrs.directPolicy, "cn"),
                    source(OfficialMrs.directPolicy, "cn", OfficialMrsRuleBehavior.IpCidr),
                ),
        )
    ),
    GeolocationNotCn(
        route(
            id = "geolocation_not_cn",
            title = "境外地理规则",
            summary = "启用 geolocation-!cn 并走 ${OfficialMrs.proxyPolicy}.",
            icon = catalogIcon("Global"),
            sources = listOf(source(OfficialMrs.proxyPolicy, "geolocation-!cn")),
        )
    ),
    Match(
        route(
            id = "match",
            title = "兜底 MATCH",
            summary = "末尾追加 ${OfficialMrs.matchRule}.",
            icon = catalogIcon("Final"),
            fixedRule = OfficialMrs.matchRule,
        )
    ),
    ;

    val id: String = definition.id
    val title: String = definition.title
    val summary: String = definition.summary
    val icon: String? = definition.icon?.url()
    val isService: Boolean = definition.isService
    val groupName: String? = definition.groupName
    val providers: List<OfficialMrsProviderSpec> = definition.providers
    val detectionRules: List<String> = definition.detectionRules
}

internal enum class OfficialMrsHealthCheckGroupType(val wireName: String) {
    UrlTest("url-test"),
    Fallback("fallback"),
}

enum class OfficialMrsRuleBehavior(val wireName: String) {
    Domain("domain"),
    IpCidr("ipcidr"),
}

data class OfficialMrsProviderSpec(
    val id: String,
    val remoteName: String,
    val behavior: OfficialMrsRuleBehavior,
)

internal val orderedRegions = OverridePresetRegion.entries.toList()
internal val orderedItems = OverridePresetItem.entries.toList()
internal val orderedServiceItems = orderedItems.filter(OverridePresetItem::isService)
private val orderedBaseItems = orderedItems.filterNot(OverridePresetItem::isService)
internal val templateProviderIds =
    orderedItems.flatMap(OverridePresetItem::providers).map(OfficialMrsProviderSpec::id).toSet()
internal val serviceGroupNames =
    orderedServiceItems.mapNotNull(OverridePresetItem::groupName).toSet()
internal val regionGroupNames =
    orderedRegions.flatMap { region -> listOf(region.groupName, region.fallbackGroupName) }.toSet()

private val defaultEnabledItems =
    listOf(
        OverridePresetItem.Proxy,
        OverridePresetItem.Ads,
        OverridePresetItem.Google,
        OverridePresetItem.Telegram,
        OverridePresetItem.GitHub,
        OverridePresetItem.Microsoft,
        OverridePresetItem.Bing,
        OverridePresetItem.Apple,
        OverridePresetItem.YouTube,
        OverridePresetItem.Netflix,
        OverridePresetItem.Spotify,
        OverridePresetItem.OpenAI,
        OverridePresetItem.Anthropic,
        OverridePresetItem.Steam,
        OverridePresetItem.Cn,
        OverridePresetItem.GeolocationNotCn,
        OverridePresetItem.Match,
    )

private val deferredRuleItems =
    listOf(
        OverridePresetItem.Proxy,
        OverridePresetItem.GeolocationNotCn,
        OverridePresetItem.Match,
    )

internal val ruleOrder: List<OverridePresetItem> =
    (orderedItems - deferredRuleItems.toSet()) + deferredRuleItems

internal val templateRules = orderedItems.flatMap(OverridePresetItem::detectionRules).toSet()

fun defaultEnabledPresetItems(): Set<OverridePresetItem> =
    defaultEnabledItems.toCollection(linkedSetOf())

fun orderedPresetRegions(): List<OverridePresetRegion> = orderedRegions

fun orderedBasePresetItems(): List<OverridePresetItem> = orderedBaseItems

fun orderedServicePresetItems(): List<OverridePresetItem> = orderedServiceItems

fun presetGroupTypeIconUrl(type: String): String? =
    when (type) {
        "urltest" -> OfficialMrs.catalogIconUrl("Urltest")
        "fallback" -> OfficialMrs.catalogIconUrl("Available")
        else -> null
    }

fun sortPresetRegions(regions: Collection<OverridePresetRegion>): List<OverridePresetRegion> {
    val selectedIds = regions.map(OverridePresetRegion::specId).toSet()
    return orderedRegions.filter { it.specId in selectedIds }
}

fun sortPresetItems(items: Collection<OverridePresetItem>): List<OverridePresetItem> {
    val selectedIds = items.map(OverridePresetItem::id).toSet()
    return orderedItems.filter { it.id in selectedIds }
}

fun defaultOverridePresetTemplateSelection(): OverridePresetTemplateSelection =
    OverridePresetTemplateSelection(
        enabledItems = defaultEnabledPresetItems(),
        enableUrlTestGroup = true,
        enableFallbackGroup = false,
    )

private class RegionDefinition(
    val specId: String,
    val displayName: String,
    val groupName: String,
    val fallbackGroupName: String,
    val keywords: String?,
    val iconName: String,
)

private fun region(
    label: String,
    displayName: String,
    keywords: String? = null,
    specId: String = label.lowercase(),
    iconName: String = label,
): RegionDefinition =
    RegionDefinition(
        specId = specId,
        displayName = displayName,
        groupName = "$label Auto",
        fallbackGroupName = "$label Fallback",
        keywords = keywords,
        iconName = iconName,
    )

private enum class PresetIconKind {
    Catalog,
    App,
}

private class PresetIcon(
    val name: String,
    val kind: PresetIconKind,
) {
    fun url(): String? =
        when (kind) {
            PresetIconKind.Catalog -> OfficialMrs.catalogIconUrl(name)
            PresetIconKind.App -> OfficialMrs.appIconUrl(name)
        }
}

private fun catalogIcon(name: String) = PresetIcon(name, PresetIconKind.Catalog)

private fun appIcon(name: String) = PresetIcon(name, PresetIconKind.App)

private class RuleSource(
    val remoteName: String,
    val behavior: OfficialMrsRuleBehavior,
    val policy: String,
)

private fun source(
    policy: String,
    remoteName: String,
    behavior: OfficialMrsRuleBehavior = OfficialMrsRuleBehavior.Domain,
): RuleSource = RuleSource(remoteName = remoteName, behavior = behavior, policy = policy)

private class PresetItemDefinition(
    val id: String,
    val title: String,
    val summary: String,
    val icon: PresetIcon?,
    val isService: Boolean,
    val groupName: String?,
    sources: List<RuleSource>,
    fixedRule: String?,
) {
    val providers: List<OfficialMrsProviderSpec> =
        sources.map { source ->
            OfficialMrsProviderSpec(
                id = source.providerId(id),
                remoteName = source.remoteName,
                behavior = source.behavior,
            )
        }

    val detectionRules: List<String> =
        buildList {
            sources.forEach { source -> add(source.detectionRule(id)) }
            fixedRule?.let(::add)
        }
}

private fun RuleSource.providerId(itemId: String): String {
    val kind =
        when (behavior) {
            OfficialMrsRuleBehavior.Domain -> "domain"
            OfficialMrsRuleBehavior.IpCidr -> "ip"
        }
    return "${itemId}_$kind"
}

private fun RuleSource.detectionRule(itemId: String): String {
    val noResolve = if (behavior == OfficialMrsRuleBehavior.IpCidr) ",no-resolve" else ""
    return "RULE-SET,${providerId(itemId)},$policy$noResolve"
}

private fun service(
    id: String,
    title: String,
    icon: PresetIcon,
    groupName: String = title,
    remoteName: String = id,
    includeIp: Boolean = false,
): PresetItemDefinition {
    val sources =
        buildList {
            add(RuleSource(remoteName, OfficialMrsRuleBehavior.Domain, groupName))
            if (includeIp) {
                add(RuleSource(remoteName, OfficialMrsRuleBehavior.IpCidr, groupName))
            }
        }
    return PresetItemDefinition(
        id = id,
        title = title,
        summary = "启用 $title 分流和专属策略组.",
        icon = icon,
        isService = true,
        groupName = groupName,
        sources = sources,
        fixedRule = null,
    )
}

private fun route(
    id: String,
    title: String,
    summary: String,
    icon: PresetIcon,
    sources: List<RuleSource> = emptyList(),
    fixedRule: String? = null,
): PresetItemDefinition =
    PresetItemDefinition(
        id = id,
        title = title,
        summary = summary,
        icon = icon,
        isService = false,
        groupName = null,
        sources = sources,
        fixedRule = fixedRule,
    )

private fun encodePathSegment(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
