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

package com.github.yumeyucca.yumebox

import android.app.Application
import android.content.ComponentName
import android.content.res.Configuration
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.common.util.PlatformIdentifier
import com.github.yumeyucca.yumebox.core.Global
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.core.util.StartupTaskCoordinator
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.data.analytics.AnonymousAnalyticsReporter
import com.github.yumeyucca.yumebox.data.controller.AppTrafficStatisticsCollector
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.di.appModule
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.screen.settings.MoeWallpaperImporter
import com.github.yumeyucca.yumebox.presentation.component.NavTrace
import com.github.yumeyucca.yumebox.substore.util.AppUtil
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.io.InputStream

class App : Application() {
    // Nothing above this scope collects the failure of a fire-and-forget task, and an unhandled
    // exception in a `launch` would tear the process down. Each step below reports its own failure;
    // this handler is the backstop for the work that cannot.
    private val startupScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default +
                CoroutineExceptionHandler { _, error ->
                    Timber.e(error, "Uncaught failure in a deferred startup task")
                },
        )

    private val geoAssetNames = listOf("geoip.metadb", "geosite.dat", "ASN.mmdb")

    /**
     * Writes the last navigation events and the fatal stack to a file the user can send back.
     *
     * TEMPORARY DIAGNOSTIC for the navigation fault: a crash in a release build prints to logcat
     * only, which is unreachable without a cable. The dump also goes to logcat unchanged, and the
     * previous handler still runs so the platform behaviour does not change.
     */
    private fun installCrashDump() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val report =
                    buildString {
                        appendLine("thread: ${thread.name}")
                        appendLine("version: ${BuildInfo.BASE_VERSION} (${BuildInfo.VERSION_CODE})")
                        appendLine()
                        appendLine("navigation:")
                        appendLine(NavTrace.dump())
                        appendLine()
                        appendLine("stack:")
                        appendLine(error.stackTraceToString())
                    }
                android.util.Log.e("YumeBoxCrash", report)
                val name = "crash-${System.currentTimeMillis()}.txt"
                listOfNotNull(getExternalFilesDir(null), cacheDir, filesDir).forEach { dir ->
                    runCatching { File(dir, name).writeText(report) }
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /**
     * Copies any crash report left by the previous run into the public Downloads folder.
     *
     * TEMPORARY DIAGNOSTIC: the dump is written where the dead process could still write, which on a
     * modern device is the app's own directory and hard to reach without a cable. Publishing it on
     * the next start makes the file reachable from any file manager. Remove with [installCrashDump].
     */
    private fun publishPendingCrashReports() {
        listOfNotNull(getExternalFilesDir(null), cacheDir, filesDir).forEach { dir ->
            val reports =
                dir.listFiles { file -> file.name.startsWith("crash-") && file.name.endsWith(".txt") }
            reports?.forEach { file ->
                if (runCatching { publishToDownloads(file) }.getOrDefault(false)) file.delete()
            }
        }
    }

    private fun publishToDownloads(file: File): Boolean {
        val values =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "yumebox-${file.name}")
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            }
        val uri =
            contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
        contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input -> input.copyTo(out) }
        } ?: return false
        return true
    }

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        installCrashDump()
        publishPendingCrashReports()
        AppVisibilityTracker.register(this)
        if (BuildInfo.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        Global.init(this)
        MMKV.initialize(this)

        // Runtime notifications/tiles jump to these activities; the runtime layer must not
        // name app classes, so the entry points are injected here.
        Components.MAIN_ACTIVITY = ComponentName(this, MainActivity::class.java)
        Components.PROXY_SHEET_ACTIVITY = ComponentName(this, ProxySheetActivity::class.java)

        val koinApp = startKoin {
            androidContext(this@App)
            modules(appModule)
        }

        val appSettingsStorage: AppSettingsStore = koinApp.koin.get()
        AppLanguageManager.apply(appSettingsStorage.appLanguage.value)

        val featureStore: FeatureStore = koinApp.koin.get()
        featureStore.syncAppVersion(BuildInfo.VERSION_CODE)
        startGeoExtraction()
        scheduleDeferredStartupTasks(koinApp.koin, featureStore, appSettingsStorage)

        PlatformIdentifier.getPlatformIdentifier()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguageManager.refreshSystemLanguage()
    }

    /**
     * Unpacks the bundled geo databases as startup warmup instead of doing it inline: a first run
     * decompresses megabytes of xz on whatever thread got here, and this runs on the main thread.
     *
     * Every path that starts the runtime awaits the warmup first, so extraction is still guaranteed to
     * finish before the core can read these files — that guarantee is the coordinator's job, not this
     * method's. It stays idempotent: existing files are left alone, so the work after a first run is
     * three `exists()` calls.
     */
    private fun startGeoExtraction() {
        StartupTaskCoordinator.startWarmup(startupScope) {
            withContext(Dispatchers.IO) { extractGeoFiles() }
        }
    }

    private fun extractGeoFiles() {
        val dir = runtimeHomeDir.apply { mkdirs() }
        for (name in geoAssetNames) {
            val target = File(dir, name)
            if (target.exists()) continue
            if (installAsset(assetName = "$name.xz", target = target, decompress = true)) continue
            installAsset(assetName = name, target = target)
        }
    }

    /**
     * Installs one bundled asset into [target], decompressing it as xz when [decompress] is set.
     *
     * A partial write is removed on failure: external-geo builds omit these runtime assets, and the
     * startup guard has to be able to require a Builtin APK to install a complete Geo data set.
     * Returns false when the asset is not bundled.
     */
    private fun installAsset(assetName: String, target: File, decompress: Boolean = false): Boolean =
        runCatching {
            assets.open(assetName).use { raw ->
                if (decompress) {
                    XZInputStream(raw.buffered()).use { xz -> target.writeFrom(xz) }
                } else {
                    target.writeFrom(raw)
                }
            }
        }
            .onFailure {
                target.delete()
                Timber.i("Runtime asset %s unavailable; a Builtin APK must install it", assetName)
            }
            .isSuccess

    private fun File.writeFrom(input: InputStream) {
        outputStream().buffered().use { output -> input.copyTo(output) }
    }

    /**
     * Best-effort self-heal for the Moe wallpaper: if the persisted
     * [AppSettingsStore.moeWallpaperUri] points at a local file:// copy that no longer exists, but
     * the original source is still recorded and readable, re-import it so the home screen keeps
     * rendering the user's choice after a cache or data wipe. Silent no-op otherwise; the render
     * path falls back to the bundled asset.
     */
    private suspend fun ensureMoeWallpaperLocalCopy(appSettings: AppSettingsStore) {
        val stored = appSettings.moeWallpaperUri.value
        if (!stored.startsWith("file://")) return
        val localPath = stored.removePrefix("file://")
        if (localPath.startsWith("/android_asset/")) return
        if (File(localPath).exists()) return
        val source = appSettings.moeWallpaperSourceUri.value
        if (source.isBlank()) return
        val reimported = MoeWallpaperImporter.importToLocal(this, source)
        if (reimported != null) {
            appSettings.moeWallpaperUri.set(reimported)
        }
    }

    /**
     * Runs one deferred startup step and reports its failure without letting it escape.
     *
     * Cancellation is not a failure and must not be logged as one: this scope lives as long as the
     * process, so the only way a step is cancelled is shutdown, and swallowing that would leave the
     * remaining steps running against a torn-down app.
     */
    private suspend fun startupStep(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, what)
        }
    }

    private fun scheduleDeferredStartupTasks(
        koin: Koin,
        featureStore: FeatureStore,
        appSettings: AppSettingsStore,
    ) {
        startupScope.launch {
            if (BuildInfo.DEBUG) return@launch
            startupStep("Failed to report anonymous app launch") {
                koin.get<AnonymousAnalyticsReporter>().reportLaunch()
            }
        }
        startupScope.launch {
            // Same ordering the inline extraction used to give: nothing that can reach the core runs
            // before the geo databases are on disk.
            StartupTaskCoordinator.awaitWarmup()
            startupStep("Failed to bootstrap custom routing default content") {
                koin.get<CustomRoutingBootstrapper>().ensureDefaultContent()
            }
            startupStep("Failed to ensure Moe wallpaper local copy") {
                ensureMoeWallpaperLocalCopy(appSettings)
            }
            // Constructing the collector is what starts it (its init launches the poll loops).
            startupStep("Failed to start app traffic statistics collection") {
                koin.get<AppTrafficStatisticsCollector>()
            }
            startupStep("Proxy group warm-up failed") {
                koin.get<ProxyFacade>().awaitProxyGroupWarmUp()
            }

            if (featureStore.isFirstTimeOpen()) {
                withContext(Dispatchers.IO) {
                    AppUtil.initFirstOpen()
                    featureStore.markFirstOpenHandled()
                }
            }
        }
    }
}
