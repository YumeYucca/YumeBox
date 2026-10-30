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

/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

@file:Suppress("ReplaceRangeToWithRangeUntil")

package com.github.yumeyucca.yumebox.presentation.util

data class FlaggedName(val countryCode: String?, val displayName: String)

private data class CountryAliases(val code: String, val names: List<String>)

private fun country(code: String, vararg names: String) = CountryAliases(code, names.toList())

/* Each ISO code owns its formal names, common aliases, and node-city names. */
private val countries =
    listOf(
        country("CN", "中国", "China", "大陆", "内地", "北京", "上海", "广州", "深圳"),
        country("HK", "香港", "Hong Kong", "HKG", "香江"),
        country("MO", "澳门", "Macau"),
        country("TW", "台湾", "Taiwan", "台北", "Taipei", "高雄"),
        country(
            "US",
            "美国",
            "United States",
            "USA",
            "America",
            "美东",
            "美西",
            "纽约",
            "New York",
            "洛杉矶",
            "Los Angeles",
            "西雅图",
            "Seattle",
            "硅谷",
        ),
        country("GB", "英国", "United Kingdom", "UK", "England", "Britain", "伦敦"),
        country("FR", "法国", "France", "巴黎"),
        country("DE", "德国", "Germany", "Deutschland", "法兰克福", "Frankfurt", "柏林"),
        country("JP", "日本", "Japan", "东京", "Tokyo", "大阪", "Osaka"),
        country("KR", "韩国", "South Korea", "Korea", "南韩", "首尔", "Seoul"),
        country("SG", "新加坡", "Singapore", "SGP", "狮城", "星国", "坡县"),
        country("AU", "澳大利亚", "澳洲", "Australia", "悉尼", "Sydney", "墨尔本", "Melbourne"),
        country("CA", "加拿大", "Canada", "多伦多", "Toronto", "温哥华", "Vancouver"),
        country("NZ", "新西兰", "New Zealand"),
        country("RU", "俄罗斯", "Russia", "莫斯科", "Moscow"),
        country("IN", "印度", "India"),
        country("ID", "印尼", "印度尼西亚", "Indonesia", "雅加达", "Jakarta"),
        country("MY", "马来西亚", "Malaysia", "大马", "吉隆坡", "Kuala Lumpur"),
        country("TH", "泰国", "Thailand", "曼谷", "Bangkok"),
        country("VN", "越南", "Vietnam", "河内", "Hanoi"),
        country("PH", "菲律宾", "Philippines", "马尼拉", "Manila"),
        country("KH", "柬埔寨", "Cambodia"),
        country("LA", "老挝", "Laos"),
        country("MM", "缅甸", "Myanmar", "Burma"),
        country("BD", "孟加拉", "Bangladesh"),
        country("PK", "巴基斯坦", "Pakistan"),
        country("LK", "斯里兰卡", "Sri Lanka"),
        country("NP", "尼泊尔", "Nepal"),
        country("BT", "不丹", "Bhutan"),
        country("MV", "马尔代夫", "Maldives"),
        country("KZ", "哈萨克斯坦", "Kazakhstan"),
        country("UZ", "乌兹别克斯坦", "Uzbekistan"),
        country("TM", "土库曼斯坦", "Turkmenistan"),
        country("TJ", "塔吉克斯坦", "Tajikistan"),
        country("KG", "吉尔吉斯斯坦", "Kyrgyzstan"),
        country("IR", "伊朗", "Iran"),
        country("IQ", "伊拉克", "Iraq"),
        country("SA", "沙特", "Saudi Arabia"),
        country("AE", "阿联酋", "UAE", "迪拜", "Dubai"),
        country("QA", "卡塔尔", "Qatar"),
        country("KW", "科威特", "Kuwait"),
        country("BH", "巴林", "Bahrain"),
        country("OM", "阿曼", "Oman"),
        country("YE", "也门", "Yemen"),
        country("JO", "约旦", "Jordan"),
        country("LB", "黎巴嫩", "Lebanon"),
        country("IL", "以色列", "Israel"),
        country("PS", "巴勒斯坦", "Palestine"),
        country("TR", "土耳其", "Turkey", "伊斯坦布尔", "Istanbul"),
        country("GE", "格鲁吉亚", "Georgia"),
        country("AZ", "阿塞拜疆", "Azerbaijan"),
        country("AM", "亚美尼亚", "Armenia"),
        country("UA", "乌克兰", "Ukraine"),
        country("BY", "白俄罗斯", "Belarus"),
        country("PL", "波兰", "Poland"),
        country("CZ", "捷克", "Czech"),
        country("SK", "斯洛伐克", "Slovakia"),
        country("HU", "匈牙利", "Hungary"),
        country("RO", "罗马尼亚", "Romania"),
        country("BG", "保加利亚", "Bulgaria"),
        country("RS", "塞尔维亚", "Serbia"),
        country("HR", "克罗地亚", "Croatia"),
        country("SI", "斯洛文尼亚", "Slovenia"),
        country("BA", "波黑", "Bosnia"),
        country("ME", "黑山", "Montenegro"),
        country("MK", "北马其顿", "Macedonia"),
        country("AL", "阿尔巴尼亚", "Albania"),
        country("GR", "希腊", "Greece"),
        country("IT", "意大利", "Italy"),
        country("ES", "西班牙", "Spain"),
        country("PT", "葡萄牙", "Portugal"),
        country("NL", "荷兰", "Netherlands", "阿姆斯特丹", "Amsterdam"),
        country("BE", "比利时", "Belgium"),
        country("LU", "卢森堡", "Luxembourg"),
        country("CH", "瑞士", "Switzerland"),
        country("AT", "奥地利", "Austria"),
        country("SE", "瑞典", "Sweden"),
        country("NO", "挪威", "Norway"),
        country("DK", "丹麦", "Denmark"),
        country("FI", "芬兰", "Finland"),
        country("IS", "冰岛", "Iceland"),
        country("IE", "爱尔兰", "Ireland"),
        country("EE", "爱沙尼亚", "Estonia"),
        country("LV", "拉脱维亚", "Latvia"),
        country("LT", "立陶宛", "Lithuania"),
        country("MX", "墨西哥", "Mexico"),
        country("BR", "巴西", "Brazil"),
        country("AR", "阿根廷", "Argentina"),
        country("CL", "智利", "Chile"),
        country("CO", "哥伦比亚", "Colombia"),
        country("PE", "秘鲁", "Peru"),
        country("VE", "委内瑞拉", "Venezuela"),
        country("EC", "厄瓜多尔", "Ecuador"),
        country("UY", "乌拉圭", "Uruguay"),
        country("PY", "巴拉圭", "Paraguay"),
        country("BO", "玻利维亚", "Bolivia"),
        country("CR", "哥斯达黎加", "Costa Rica"),
        country("PA", "巴拿马", "Panama"),
        country("CU", "古巴", "Cuba"),
        country("JM", "牙买加", "Jamaica"),
        country("DO", "多米尼加", "Dominican"),
        country("EG", "埃及", "Egypt"),
        country("ZA", "南非", "South Africa"),
        country("NG", "尼日利亚", "Nigeria"),
        country("KE", "肯尼亚", "Kenya"),
        country("MA", "摩洛哥", "Morocco"),
        country("TN", "突尼斯", "Tunisia"),
        country("DZ", "阿尔及利亚", "Algeria"),
        country("LY", "利比亚", "Libya"),
        country("ET", "埃塞俄比亚", "Ethiopia"),
        country("GH", "加纳", "Ghana"),
        country("TZ", "坦桑尼亚", "Tanzania"),
        country("UG", "乌干达", "Uganda"),
        country("RW", "卢旺达", "Rwanda"),
        country("ZW", "津巴布韦", "Zimbabwe"),
        country("BW", "博茨瓦纳", "Botswana"),
        country("NA", "纳米比亚", "Namibia"),
        country("FJ", "斐济", "Fiji"),
        country("PG", "巴布亚新几内亚", "Papua New Guinea"),
        country("KP", "朝鲜", "North Korea"),
        country("MN", "蒙古", "Mongolia"),
        country("BN", "文莱", "Brunei"),
        country("TL", "东帝汶", "Timor"),
    )

private val countryNameToCode = buildMap {
    countries.forEach { country ->
        country.names.forEach { name -> put(name.lowercase(), country.code) }
    }
}
private val countryNameRegex =
    Regex(
        countryNameToCode.keys.sortedByDescending(String::length).joinToString("|") {
            Regex.escape(it)
        },
        RegexOption.IGNORE_CASE,
    )
private val countryCodes = countries.mapTo(mutableSetOf()) { it.code }
// Both sides exclude digits as well: "LA01"/"ID-1" are node names, not los-angeles/indonesia.
private val countryCodeAbbreviationRegex = Regex("(?<![A-Za-z0-9])([A-Za-z]{2})(?![A-Za-z0-9])")
private const val regionalIndicatorBase = 0x1F1E6
private const val regionalIndicatorEnd = 0x1F1FF

private fun Char.isNameSeparator() = isWhitespace() || this in "-|·•—:"

private fun isRegionalIndicator(codePoint: Int) =
    codePoint in regionalIndicatorBase..regionalIndicatorEnd

private fun findCountryCodeFromName(name: String) =
    countryNameRegex.find(name)?.value?.lowercase()?.let(countryNameToCode::get)

private fun findCountryCodeAbbreviation(name: String) =
    countryCodeAbbreviationRegex
        .findAll(name)
        .map { it.groupValues[1].uppercase() }
        .firstOrNull(countryCodes::contains)

private fun findFlagEmojiCountryCode(text: String): Pair<String, IntRange>? {
    var index = 0
    while (index < text.length - 1) {
        val first = text.codePointAt(index)
        val firstLength = Character.charCount(first)
        val secondIndex = index + firstLength
        if (isRegionalIndicator(first) && secondIndex < text.length) {
            val second = text.codePointAt(secondIndex)
            if (isRegionalIndicator(second)) {
                val code =
                    "${('A'.code + first - regionalIndicatorBase).toChar()}${('A'.code + second - regionalIndicatorBase).toChar()}"
                return code to index..(secondIndex + Character.charCount(second) - 1)
            }
        }
        index += firstLength
    }
    return null
}

fun extractFlaggedName(rawName: String): FlaggedName {
    val name = rawName.trim()
    if (name.isEmpty()) return FlaggedName(null, rawName)
    findFlagEmojiCountryCode(name)?.let { (code, range) ->
        val before = name.substring(0, range.first).trimEnd(Char::isNameSeparator)
        val after = name.substring(range.last + 1).trimStart(Char::isNameSeparator)
        return FlaggedName(
            code,
            listOf(before, after).filter(String::isNotEmpty).joinToString(" ").ifEmpty { name },
        )
    }
    return FlaggedName(findCountryCodeFromName(name) ?: findCountryCodeAbbreviation(name), name)
}
