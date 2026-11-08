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
 */

package com.github.yumeyucca.yumebox.runtime.service.session

import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.core.util.YamlCodec
import java.io.File

/** Adds the optional CN provider and native eBPF listener required for CN bypass. */
object EbpfOverride {
    const val FILE_NAME = "__ebpf_cn_override__.yaml"
    private const val LISTENER_NAME = "ebpf-in"
    private const val CN_PROVIDER_NAME = "CN-IP"
    private const val CN_PROVIDER_URL = "https://github.com/MetaCubeX/meta-rules-dat/raw/meta/geo/geoip/cn.mrs"

    data class Config(val bypassCn: Boolean)

    fun buildYaml(config: Config, providerPath: String): String {
        if (!config.bypassCn) return ""
        val override =
            linkedMapOf<String, Any?>(
                "listeners+" to
                    listOf(
                        linkedMapOf<String, Any?>(
                            "name" to LISTENER_NAME,
                            "type" to "ebpf",
                            "mode" to "hybrid",
                            "dns-mode" to "hijack",
                            "bypass-rule-set" to listOf(CN_PROVIDER_NAME),
                            "shared" to linkedMapOf("interface" to listOf("br0")),
                        )
                    ),
                "rule-providers-merge" to
                    linkedMapOf(
                        CN_PROVIDER_NAME to
                            linkedMapOf<String, Any?>(
                                "type" to "http",
                                "behavior" to "ipcidr",
                                "format" to "mrs",
                                "interval" to 86400,
                                "path" to providerPath,
                                "url" to CN_PROVIDER_URL,
                            )
                    ),
            )
        return YamlCodec.dumpMap(override)
    }

    fun materialize(config: Config, dir: File): OverrideSpec? {
        if (!config.bypassCn) return null
        dir.mkdirs()
        val file = File(dir, FILE_NAME)
        val providerFile = File(dir, "rule_provider/cn-ip.mrs")
        providerFile.parentFile?.mkdirs()
        file.writeText(buildYaml(config, providerFile.absolutePath))
        return OverrideSpec(path = file.absolutePath, ext = "yaml")
    }
}
