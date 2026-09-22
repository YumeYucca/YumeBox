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

package com.github.yumeyucca.yumebox.runtime.service.notification

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

/**
 * Builds the HyperOS Super Island payload that the system reads from the
 * `miui.focus.param` notification extra.
 *
 * The structure mirrors what the HyperOS island renderer expects for a long-running session: a
 * base info block with the profile name and the subscription line, a hint block for the expanded
 * area with the current node and the compact traffic text, and the island area definitions
 * (big island template with the profile name and the compact text, small island with the app icon).
 */
object MiuiIslandParams {
    private const val BUSINESS = "yumebox_service"

    /**
     * Monotonic island sequence, seeded once per process: the island renderer drops updates whose
     * sequence did not grow, and a freshly started process must not replay the sequence of the
     * previous one.
     */
    private val sequence = AtomicLong(System.currentTimeMillis() / 1000)

    fun build(
        profileName: String,
        usageText: String,
        compactText: String,
        currentNode: String?,
    ): String {
        val now = System.currentTimeMillis()
        val node = currentNode ?: "未选择"

        val params =
            JSONObject().apply {
                put("business", BUSINESS)
                put("protocol", 1)
                put("islandFirstFloat", true)
                put("enableFloat", false)
                put("updatable", true)
                put("outEffectSrc", "")
                put("reopen", "reopen")
                put("sequence", sequence.incrementAndGet())
                put("aodTitle", profileName)
                put(
                    "baseInfo",
                    JSONObject().apply {
                        put("type", 2)
                        put("title", profileName)
                        // One line under the title: percentage, usage and expiry joined with "|".
                        put("content", usageText)
                        put("subTitle", "")
                        put("extraTitle", "")
                        put("specialTitle", "")
                        put("subContent", "")
                        put("picFunction", "")
                        put("showDivider", true)
                        put("showContentDivider", false)
                        put("colorTitle", "#111111")
                        put("colorTitleDark", "#ffffff")
                        put("colorContent", "#333333")
                        put("colorContentDark", "#cccccc")
                    },
                )
                put(
                    "picInfo",
                    JSONObject().apply {
                        put("type", 1)
                        put("pic", "")
                    },
                )
                put(
                    "hintInfo",
                    JSONObject().apply {
                        put("type", 2)
                        put("content", "当前节点")
                        put("title", node)
                        put(
                            "timerInfo",
                            JSONObject().apply {
                                put("timerType", 0)
                                put("timerWhen", 0L)
                                put("timerTotal", 0L)
                                put("timerSystemCurrent", now)
                            },
                        )
                        put("subContent", "实时流量")
                        put("subTitle", compactText)
                        put("colorContent", "#666666")
                        put("colorContentDark", "#aaaaaa")
                        put("colorTitle", "#222222")
                        put("colorTitleDark", "#eeeeee")
                        put("colorSubContent", "#666666")
                        put("colorSubContentDark", "#aaaaaa")
                        put("colorSubTitle", "#222222")
                        put("colorSubTitleDark", "#eeeeee")
                    },
                )
                put(
                    "param_island",
                    JSONObject().apply {
                        put("islandProperty", 1)
                        put("islandTimeout", 3600)
                        put(
                            "bigIslandArea",
                            JSONObject().apply {
                                put("templateNo", 2)
                                put(
                                    "imageTextInfoLeft",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put(
                                            "textInfo",
                                            JSONObject().apply {
                                                put("title", profileName)
                                                put("content", "")
                                                put("showHighlightColor", false)
                                                put("narrowFont", false)
                                            },
                                        )
                                    },
                                )
                                put(
                                    "textInfo",
                                    JSONObject().apply {
                                        put("frontTitle", "")
                                        put("title", compactText)
                                        put("content", "")
                                        put("showHighlightColor", false)
                                        put("narrowFont", false)
                                    },
                                )
                            },
                        )
                        put(
                            "smallIslandArea",
                            JSONObject().apply {
                                put(
                                    "picInfo",
                                    JSONObject().apply {
                                        put("type", 1)
                                        put("pic", "miui.focus.pic_small")
                                        put("picDark", "miui.focus.pic_small_dark")
                                    },
                                )
                            },
                        )
                    },
                )
            }

        return JSONObject().put("param_v2", params).toString()
    }
}
