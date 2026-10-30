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

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.common.util.AutoStartDependencies
import com.github.yumeyucca.yumebox.common.util.IntentController
import com.github.yumeyucca.yumebox.common.util.startActivitySafely
import com.github.yumeyucca.yumebox.common.util.ProxyAutoStartHelper
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.core.util.StartupTaskCoordinator
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.di.APPLICATION_SCOPE_NAME
import com.github.yumeyucca.yumebox.presentation.component.YumeAppShell
import com.github.yumeyucca.yumebox.presentation.navigation.DeepLink
import com.github.yumeyucca.yumebox.presentation.navigation.deepLinkOf
import com.github.yumeyucca.yumebox.presentation.navigation.AppNavContainer
import com.github.yumeyucca.yumebox.presentation.navigation.AppNavigationComponent
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.service.WifiAutomationService
import com.github.yumeyucca.yumebox.screen.moe.HomePreviewGuideDialog
import com.github.yumeyucca.yumebox.screen.moe.SystemWallpaperAccess
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.qualifier.named
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {
    private var systemWallpaperPermissionRequestHandled = false
    private val appSettingsStore: AppSettingsStore by inject()
    private val navigationComponent: AppNavigationComponent by lazy {
        retainedComponent { componentContext ->
            AppNavigationComponent(
                componentContext = componentContext,
                predictiveBackEnabledAtLaunch = appSettingsStore.predictiveBackEnabled.value,
            )
        }
    }

    companion object {
        private const val REQUEST_STARTUP_PERMISSIONS = 1001
        private const val REQUEST_SYSTEM_WALLPAPER_PERMISSION = 1002
        private val _pendingImportUrl = MutableStateFlow<String?>(null)
        val pendingImportUrl: StateFlow<String?> = _pendingImportUrl.asStateFlow()

        fun clearPendingImportUrl() {
            _pendingImportUrl.value = null
        }

        private val _pendingDeepLink = MutableStateFlow<DeepLink?>(null)
        val pendingDeepLink: StateFlow<DeepLink?> = _pendingDeepLink.asStateFlow()

        /** Consumes [link] only while it is still the pending one; a link that arrived since stays. */
        fun clearPendingDeepLink(link: DeepLink) {
            if (_pendingDeepLink.value === link) _pendingDeepLink.value = null
        }
    }

    private val featureStore: FeatureStore by inject()
    private val networkSettingsStorage: NetworkSettingsStore by inject()
    private val profilesRepository: ProfilesRepository by inject()
    private val proxyFacade: ProxyFacade by inject()
    private val serviceCache: MMKV by inject(qualifier = named("service_cache"))
    private val applicationScope: CoroutineScope by inject(qualifier = named(APPLICATION_SCOPE_NAME))

    private lateinit var intentController: IntentController

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)
        applyExcludeFromRecents(appSettingsStore.excludeFromRecents.value)

        intentController = IntentController(lifecycleScope)
        // The launch intent survives an Activity recreation, so handling it again after a rotation
        // would re-deliver a link the user already acted on. A fresh create, on the other hand, is
        // always a real delivery, and links that arrive while we are running come through
        // [onNewIntent], which is the only other path that publishes one.
        if (savedInstanceState == null) handleIntent(intent)

        val startupPermissionsRequested = requestStartupPermissions()
        if (!startupPermissionsRequested) {
            window.decorView.post(::requestSystemWallpaperPermissionIfNeeded)
        }

        if (networkSettingsStorage.wifiAutomationEnabled.value) {
            WifiAutomationService.start(this)
        }

        val showHomeGuideInitially =
            savedInstanceState == null && !appSettingsStore.homePreviewGuideShown.value

        showAppContent(showHomeGuideInitially)

        launchAutoStart()
    }

    private fun showAppContent(showHomeGuideInitially: Boolean) {
        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            MainActivityContent(
                appSettingsViewModel = appSettingsViewModel,
                showHomeGuideInitially = showHomeGuideInitially,
                onExcludeFromRecentsChange = ::applyExcludeFromRecents,
                onHomePreviewGuideShown = { appSettingsStore.homePreviewGuideShown.set(true) },
                navigationComponent = navigationComponent,
            )
        }
    }

    /**
     * Starts the proxy if auto start is due, after the startup warm-up gate opens.
     *
     * [applicationScope] outlives this Activity, so nothing here may read instance members that
     * point at it — the dependencies are resolved into [AutoStartDependencies] first and only the
     * application context is captured.
     */
    private fun launchAutoStart() {
        val autoStartDependencies =
            AutoStartDependencies(
                featureStore = featureStore,
                proxyFacade = proxyFacade,
                profilesRepository = profilesRepository,
                appSettingsStorage = appSettingsStore,
                networkSettingsStorage = networkSettingsStorage,
                serviceCache = serviceCache,
            )
        val autoStartContext = appContextOrSelf
        applicationScope.launch {
            if (!AutoStartSessionGate.tryBeginAutoActions()) {
                return@launch
            }
            var handled = false
            try {
                StartupTaskCoordinator.awaitWarmup()
                with(autoStartDependencies) {
                    ProxyAutoStartHelper.checkAndAutoStart(autoStartContext)
                }
                handled = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Auto-start after warm-up failed")
            } finally {
                AutoStartSessionGate.finishAutoActions(markHandled = handled)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep [getIntent] naming the newest link so a later recreation reads the same target.
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STARTUP_PERMISSIONS) {
            window.decorView.post(::requestSystemWallpaperPermissionIfNeeded)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level < TRIM_MEMORY_UI_HIDDEN || isFinishing) {
            return
        }
        if (!featureStore.exitUiWhenBackground.value) {
            return
        }
        if (proxyFacade.runtimeSnapshot.value.running) {
            finishAndRemoveTask()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val safeIntent = intent ?: return
        safeIntent.data?.let { handleDeepLinkUri(it) }

        intentController.handleIntent(safeIntent)
    }

    private fun handleDeepLinkUri(uri: Uri) {
        when (uri.scheme) {
            "clash",
            "clashmeta",
            -> {
                if (uri.host != "install-config") return
                val configUrl = uri.getQueryParameter("url")
                if (!configUrl.isNullOrBlank()) {
                    _pendingImportUrl.value = configUrl
                }
            }

            "yumebox" -> {
                deepLinkOf(uri)?.let { _pendingDeepLink.value = it }
            }
        }
    }

    /** Requests only the notification permission that is relevant during application startup. */
    private fun requestStartupPermissions(): Boolean {
        val permissions =
            buildList {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_STARTUP_PERMISSIONS)
        }
        return permissions.isNotEmpty()
    }

    private fun requestSystemWallpaperPermissionIfNeeded() {
        if (systemWallpaperPermissionRequestHandled) return
        systemWallpaperPermissionRequestHandled = true
        if (!appSettingsStore.useSystemWallpaper.value) return
        appSettingsStore.systemWallpaperPermissionRequested.set(true)
        if (SystemWallpaperAccess.isGranted(this)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startActivitySafely(SystemWallpaperAccess.settingsIntent(this))
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_SYSTEM_WALLPAPER_PERMISSION,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun applyExcludeFromRecents(exclude: Boolean) {
        runCatching {
            val am = getSystemService(ActivityManager::class.java) ?: return@runCatching
            val currentTaskId = taskId
            val task =
                am.appTasks.firstOrNull { appTask: ActivityManager.AppTask ->
                    val taskInfo = appTask.taskInfo ?: return@firstOrNull false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        taskInfo.taskId == currentTaskId
                    } else {
                        taskInfo.id == currentTaskId
                    }
                }
            task?.setExcludeFromRecents(exclude)
        }
    }
}

/**
 * The Activity's whole UI tree: theme shell, density scaling and the one dialog that outlives
 * navigation. Kept out of `onCreate` so the Activity method stays a lifecycle body.
 */
@Composable
private fun MainActivityContent(
    appSettingsViewModel: AppSettingsViewModel,
    showHomeGuideInitially: Boolean,
    onExcludeFromRecentsChange: (Boolean) -> Unit,
    onHomePreviewGuideShown: () -> Unit,
    navigationComponent: AppNavigationComponent,
) {
    val excludeFromRecents by appSettingsViewModel.excludeFromRecents.state.collectAsState()
    val pageScale by appSettingsViewModel.pageScale.state.collectAsState()
    val useSystemWallpaper by appSettingsViewModel.useSystemWallpaper.state.collectAsState()

    LaunchedEffect(excludeFromRecents) { onExcludeFromRecentsChange(excludeFromRecents) }

    YumeAppShell {
        val systemDensity = LocalDensity.current
        val scaledDensity =
            remember(systemDensity, pageScale) {
                Density(systemDensity.density * pageScale, systemDensity.fontScale)
            }
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MiuixTheme.colorScheme.surface,
            ) {
                AppNavContainer(navigationComponent)

                var showHomeGuide by remember { mutableStateOf(showHomeGuideInitially) }
                HomePreviewGuideDialog(
                    show = showHomeGuide,
                    useSystemWallpaper = useSystemWallpaper,
                    onUseSystemWallpaperChange = appSettingsViewModel::onUseSystemWallpaperChange,
                    onDismissRequest = {
                        showHomeGuide = false
                        onHomePreviewGuideShown()
                    },
                )
            }
        }
    }
}
