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

package com.github.yumeyucca.yumebox.screen.settings

import android.content.Context
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.os.Build
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.github.yumeyucca.yumebox.screen.settings.ChinaAppDetector.Companion.MAX_SCANNABLE_DEX_BYTES
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.zip.ZipFile

/**
 * Heuristic detector for apps of Chinese origin, used by the access-control region quick select.
 * Detection escalates through four tiers:
 *
 * 1. skip allowlist (well-known overseas vendors) -> not China;
 * 2. package-name heuristics (`cn.` segments + vendor/SDK prefix regex) -> China;
 * 3. manifest component class names matched against the same prefix regex (catches apps embedding
 *    Tencent/Umeng/Bugly/... SDK components);
 * 4. APK deep scan: a `firebase-*` zip entry short-circuits to NOT China, otherwise dex class names
 *    are prefix-matched against the China vendor/SDK package prefixes; oversized (>100 MB) or
 *    unparsable dex entries are skipped as inconclusive.
 *
 * Tier 3/4 verdicts are cached in a dedicated MMKV keyed by package name and invalidated via the
 * app's lastUpdateTime, so only the first scan after an (un)install is expensive.
 */
class ChinaAppDetector(context: Context) {
    data class Candidate(val packageName: String, val lastUpdateTime: Long)

    private val packageManager: PackageManager = context.packageManager

    private val cache by lazy { MMKV.mmkvWithID(CACHE_ID) }

    /**
     * Deep scans buffer whole dex entries in memory (up to [MAX_SCANNABLE_DEX_BYTES] each), so
     * unbounded parallelism could transiently hold hundreds of MB. Fast-path prefix checks stay
     * fully parallel; only the expensive scan path is throttled.
     */
    private val deepScanPermits = Semaphore(MAX_CONCURRENT_DEEP_SCANS)

    suspend fun detectChinaPackages(candidates: List<Candidate>): Set<String> = coroutineScope {
        candidates
            .map { candidate ->
                async(Dispatchers.Default) { candidate.takeIf { isChinaPackage(it) } }
            }
            .awaitAll()
            .filterNotNull()
            .mapTo(linkedSetOf()) { it.packageName }
    }

    private suspend fun isChinaPackage(candidate: Candidate): Boolean {
        val normalized = candidate.packageName.lowercase()
        skipPrefixList.forEach {
            if (normalized == it || normalized.startsWith("$it.")) {
                return false
            }
        }
        if (
            normalized.startsWith("cn.") ||
            normalized.contains(".cn.") ||
            normalized.endsWith(".cn")
        ) {
            return true
        }
        if (normalized.matches(chinaAppRegex)) {
            return true
        }
        return deepScanPermits.withPermit { cachedDeepScan(candidate) }
    }

    private fun cachedDeepScan(candidate: Candidate): Boolean {
        cache.decodeString(candidate.packageName)?.let { cached ->
            val (stamp, verdict) =
                cached.split('|', limit = 2).takeIf { it.size == 2 } ?: return@let
            if (stamp == candidate.lastUpdateTime.toString()) {
                return verdict.toBoolean()
            }
        }
        val verdict = deepScan(candidate.packageName)
        cache.encode(candidate.packageName, "${candidate.lastUpdateTime}|$verdict")
        return verdict
    }

    private fun deepScan(packageName: String): Boolean {
        return try {
            val flags =
                PackageManager.MATCH_UNINSTALLED_PACKAGES or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(flags.toLong()),
                    )
                } else {
                    @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, flags)
                }
            val components =
                buildList<ComponentInfo> {
                    packageInfo.activities?.let { addAll(it) }
                    packageInfo.services?.let { addAll(it) }
                    packageInfo.receivers?.let { addAll(it) }
                    packageInfo.providers?.let { addAll(it) }
                }
            if (components.any { it.name.matches(chinaAppRegex) }) {
                return true
            }
            val apkPath = packageInfo.applicationInfo?.publicSourceDir ?: return false
            scanApkDexClasses(File(apkPath))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // One unreadable package must not abort the batch: its dex files may be gone, the app may
            // be mid-update, or the package manager may refuse the query outright.
            false
        }
    }

    private fun scanApkDexClasses(apk: File): Boolean {
        ZipFile(apk).use { zip ->
            for (entry in zip.entries()) {
                if (entry.name.startsWith("firebase-")) {
                    return false
                }
            }
            for (entry in zip.entries()) {
                if (!(entry.name.startsWith("classes") && entry.name.endsWith(".dex"))) {
                    continue
                }
                // DexBackedDexFile.fromInputStream buffers the whole entry in memory, so
                // parsing arbitrarily large dex risks OOM on low-end devices; skip as inconclusive.
                if (entry.size > MAX_SCANNABLE_DEX_BYTES) {
                    continue
                }
                val dexFile =
                    try {
                        DexBackedDexFile.fromInputStream(null, zip.getInputStream(entry).buffered())
                    } catch (_: Exception) {
                        continue
                    }
                if (dexContainsChinaClass(dexFile)) {
                    return true
                }
            }
        }
        return false
    }

    private fun dexContainsChinaClass(dexFile: DexBackedDexFile): Boolean {
        for (clazz in dexFile.classes) {
            val descriptor = clazz.type
            if (chinaClassDescriptorPrefixList.any { descriptor.startsWith(it) }) {
                return true
            }
        }
        return false
    }

    companion object {
        // The version suffix invalidates cached verdicts produced by older heuristics.
        private const val CACHE_ID = "china_app_detector_cache_v2"

        // Upper bound for dex entries we are willing to buffer in memory for scanning.
        private const val MAX_SCANNABLE_DEX_BYTES = 100L * 1024 * 1024

        // Bounds peak scan memory to MAX_CONCURRENT_DEEP_SCANS * MAX_SCANNABLE_DEX_BYTES.
        private const val MAX_CONCURRENT_DEEP_SCANS = 2

        private val skipPrefixList =
            listOf(
                "com.google",
                "com.android.chrome",
                "com.android.vending",
                "com.microsoft",
                "com.apple",
                "com.zhiliaoapp.musically",
                "com.android.providers.downloads",
            )

        private val chinaAppPrefixList =
            listOf(
                "com.tencent",
                "com.tencent.mobileqq",
                "com.tencent.mm",
                "com.tencent.qqlive",
                "com.tencent.news",
                "com.tencent.wework",
                "com.tencent.weishi",
                "com.tencent.karaoke",
                "com.tencent.qqmusic",
                "com.alibaba",
                "com.alibaba.android",
                "com.alibaba.wireless",
                "com.alibaba.rimet",
                "com.umeng",
                "com.qihoo",
                "com.ali",
                "com.alipay",
                "com.amap",
                "com.sina",
                "com.weibo",
                "com.sankuai",
                "com.sankuai.meituan",
                "com.sankuai.meituan.takeoutnew",
                "com.dianping",
                "com.jingdong",
                "com.xunmeng",
                "com.xingin",
                "com.zhihu",
                "com.bilibili",
                "com.coolapk",
                "tv.danmaku",
                "com.kuaishou",
                "com.smile.gifmaker",
                "com.ss.android",
                "com.ss.android.ugc",
                "com.ss.android.article",
                "com.qiyi",
                "com.youku",
                "com.youku.phone",
                "com.sohu",
                "com.autonavi",
                "com.sogou",
                "com.sogou.inputmethod",
                "com.iflytek",
                "com.iflytek.inputmethod",
                "com.kingsoft",
                "com.qzone",
                "com.vivo",
                "com.xiaomi",
                "com.huawei",
                "com.taobao",
                "com.taobao.idlefish",
                "com.secneo",
                "s.h.e.l.l",
                "com.stub",
                "com.kiwisec",
                "com.secshell",
                "com.wrapper",
                "cn.securitystack",
                "com.mogosec",
                "com.secoen",
                "com.netease",
                "com.mx",
                "com.qq.e",
                "com.baidu",
                "com.bytedance",
                "com.bugly",
                "com.miui",
                "com.oppo",
                "com.coloros",
                "com.iqoo",
                "com.meizu",
                "com.gionee",
                "com.oplus",
                "andes.oplus",
                "com.unionpay",
            )

        private val chinaAppRegex by lazy {
            ("(" + chinaAppPrefixList.joinToString("|").replace(".", "\\.") + ").*").toRegex()
        }

        // "com.tencent" -> "Lcom/tencent/" so dex class descriptors can be prefix-matched
        // without per-class string transforms or regex.
        private val chinaClassDescriptorPrefixList by lazy {
            chinaAppPrefixList.map { "L" + it.replace('.', '/') + "/" }
        }
    }
}
