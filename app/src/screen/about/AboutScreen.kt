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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.about


import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.common.util.openUrl
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.isDarkTheme
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.koin.compose.koinInject
import timber.log.Timber

private val AboutHeroSpacerExtraHeight = 230.dp
private const val TitleFadeStart = 0.35f
private const val TitleFadeRange = 0.65f
private const val AboutCardBlurRadius = 60f
private const val AboutTopBarBlurRadius = 25f

@Composable
fun AboutScreen(navigator: Navigator) {
    val context = LocalContext.current
    val appSettingsStore = koinInject<AppSettingsStore>()
    var showDebugPanel by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val exportLogsLauncher = rememberDiagnosticsExportLauncher()
    val backdrop = rememberLayerBackdrop()
    val blur = remember { isRuntimeShaderSupported() }
    val isDark = isDarkTheme()
    val logoBlend = remember(isDark) { logoBlendColors(isDark) }
    val cardBlend = remember(isDark) { cardBlendColors(isDark) }
    val heroMetrics = remember { AboutHeroMetrics() }
    val scrollProgress by rememberAboutScrollProgress(listState, heroMetrics)
    val heroProgress by rememberAboutHeroProgress(listState, heroMetrics)
    val linkGroups =
        aboutLinkGroups(
            context = context,
            navigator = navigator,
            onExportLogs = {
                exportLogsLauncher.launch("yumebox_diagnostics_${System.currentTimeMillis()}.log")
            },
        )

    GlassScaffold(
        topBar = {
            AboutTopBar(scrollBehavior = scrollBehavior, progress = scrollProgress, blur = blur)
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            AboutDynamicBackground(
                listState = listState, backdrop = backdrop,
                blur = blur, scrollProgress = scrollProgress,
            )
            AboutPageList(
                scrollBehavior = scrollBehavior, innerPadding = innerPadding,
                listState = listState, metrics = heroMetrics,
                showDebugPanel = showDebugPanel, appSettingsStore = appSettingsStore,
                linkGroups = linkGroups, backdrop = backdrop, blur = blur,
                blendColors = cardBlend,
            )
            AboutHeroLayer(
                coreVersion = BuildInfo.CORE_VERSION,
                topPadding = innerPadding.calculateTopPadding(),
                backdrop = if (blur) backdrop else null,
                logoBlend = logoBlend, progress = heroProgress,
                metrics = heroMetrics, onShowDebugPanel = { showDebugPanel = true },
            )
        }
    }
}

/**
 * The diagnostics file picker: it writes the log bundle off the main thread and reports the outcome
 * from the main one.
 */
@Composable
private fun rememberDiagnosticsExportLauncher(): ManagedActivityResultLauncher<String, Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val success = exportStartupLogs(context, uri)
            withContext(Dispatchers.Main) {
                context.toast(
                    if (success) YumeTxt.About.Support.ExportSuccess
                    else YumeTxt.About.Support.ExportFailed
                )
            }
        }
    }
}

/** How far the hero has scrolled away: 0 while it is fully visible, 1 once it is gone. */
@Composable
private fun rememberAboutScrollProgress(
    listState: LazyListState,
    heroMetrics: AboutHeroMetrics,
): State<Float> =
    remember(listState, heroMetrics) {
        derivedStateOf {
            when {
                heroMetrics.spacerHeightPx <= 0 -> 0f
                listState.firstVisibleItemIndex > 0 -> 1f
                else ->
                    (listState.firstVisibleItemScrollOffset.toFloat() / heroMetrics.spacerHeightPx)
                        .coerceIn(0f, 1f)
            }
        }
    }

/** The hero's own collapse progress, read from the list's first visible item. */
@Composable
private fun rememberAboutHeroProgress(
    listState: LazyListState,
    heroMetrics: AboutHeroMetrics,
): State<AboutHeroProgress> =
    remember(listState, heroMetrics) {
        derivedStateOf {
            heroMetrics.progressAt(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
            )
        }
    }

/** The page list: the spacer the hero floats over, then the debug panel, cards and footer. */
@Composable
private fun AboutPageList(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    listState: LazyListState,
    metrics: AboutHeroMetrics,
    showDebugPanel: Boolean,
    appSettingsStore: AppSettingsStore,
    linkGroups: List<List<AboutLinkRow>>,
    backdrop: LayerBackdrop,
    blur: Boolean,
    blendColors: List<BlendColorEntry>,
) {
    val spacerHeight = metrics.heroHeight + AboutHeroSpacerExtraHeight
    StandalonePageList(
        scrollBehavior = scrollBehavior,
        scaffoldPadding = innerPadding,
        lazyListState = listState,
    ) {
        item(key = "logoSpacer") {
            AboutHeroSpacer(height = spacerHeight, metrics = metrics, listState = listState)
        }
        item(key = "aboutContent") {
            AboutContentColumn(
                listState = listState,
                showDebugPanel = showDebugPanel,
                appSettingsStore = appSettingsStore,
                linkGroups = linkGroups,
                backdrop = backdrop,
                blur = blur,
                blendColors = blendColors,
            )
        }
    }
}

/** The screen's top bar: it fades its title in and only blurs once the hero has gone. */
@Composable
private fun AboutTopBar(
    scrollBehavior: ScrollBehavior,
    progress: Float,
    blur: Boolean,
) {
    val surface = MiuixTheme.colorScheme.surface
    // The top bar only starts to blur/back-fill once the hero has scrolled away entirely.
    val heroCollapsed = progress == 1f
    val barBackdrop = LocalTopBarBackdrop.current?.takeIf { blur && heroCollapsed }
    val barColor = if (heroCollapsed && barBackdrop == null) surface else Color.Transparent
    val titleAlpha = ((progress - TitleFadeStart) / TitleFadeRange).coerceIn(0f, 1f)
    Box(
        modifier =
            if (barBackdrop != null) {
                Modifier.textureBlur(
                    backdrop = barBackdrop,
                    shape = RectangleShape,
                    blurRadius = AboutTopBarBlurRadius,
                    colors =
                        BlurColors(
                            blendColors = listOf(BlendColorEntry(surface.copy(alpha = 0.87f))),
                        ),
                )
            } else {
                Modifier
            },
    ) {
        SmallTopAppBar(
            title = YumeTxt.About.Title,
            scrollBehavior = scrollBehavior,
            color = barColor,
            titleColor = MiuixTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
        )
    }
}

/** The wallpaper layer behind the list: it fades out with the hero while the page scrolls. */
@Composable
private fun AboutDynamicBackground(
    listState: LazyListState,
    backdrop: LayerBackdrop,
    blur: Boolean,
    scrollProgress: Float,
) {
    // Keyed on the list: the state object is re-created when the page is re-entered, and an unkeyed
    // derived state would keep reading the list that is no longer on screen.
    val visible by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    BgEffectBackground(
        dynamicBackground = visible,
        modifier = Modifier.fillMaxSize(),
        bgModifier = if (blur) Modifier.layerBackdrop(backdrop) else Modifier,
        isFullSize = true,
        alpha = { 1f - scrollProgress },
    )
}

/** The blank run of list the hero floats over; it reports its height back to [metrics]. */
@Composable
private fun AboutHeroSpacer(
    height: Dp,
    metrics: AboutHeroMetrics,
    listState: LazyListState,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { metrics.updateSpacerHeightPx(it.height) }
            .onGloballyPositioned { coords ->
                metrics.updateSpacerBottom(
                    bottom = coords.positionInWindow().y + coords.size.height,
                    captureInitial =
                        listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset > 0,
                )
            },
    )
}

/** The page body: debug panel, link cards and footer, kept at least as tall as the viewport. */
@Composable
private fun AboutContentColumn(
    listState: LazyListState,
    showDebugPanel: Boolean,
    appSettingsStore: AppSettingsStore,
    linkGroups: List<List<AboutLinkRow>>,
    backdrop: LayerBackdrop,
    blur: Boolean,
    blendColors: List<BlendColorEntry>,
) {
    val density = LocalDensity.current
    val contentMinHeight by remember(density, listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val available =
                (info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding)
                    .coerceAtLeast(0)
            with(density) { available.toDp() }
        }
    }
    Column(Modifier.fillMaxWidth().heightIn(min = contentMinHeight)) {
        AboutDebugPanel(visible = showDebugPanel, appSettingsStore = appSettingsStore)
        linkGroups.forEach { rows ->
            AboutLinkCard(backdrop, blur, blendColors, rows)
        }
        AboutCopyrightFooter()
    }
}

/** The hidden debug card, revealed by five quick taps on the version line. */
@Composable
private fun AboutDebugPanel(visible: Boolean, appSettingsStore: AppSettingsStore) {
    val context = LocalContext.current
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            AppCard {
                PreferenceArrowItem(
                    title = YumeTxt.About.Debug.TestInitialization,
                    onClick = {
                        appSettingsStore.homePreviewGuideShown.set(false)
                        appSettingsStore.systemWallpaperPermissionRequested.set(false)
                        context.toast(YumeTxt.About.Debug.InitializationReset)
                    },
                )
            }
            Spacer(Modifier.height(UiDp.dp12))
        }
    }
}

/** The centred copyright line that closes the page. */
@Composable
private fun AboutCopyrightFooter() {
    Spacer(Modifier.height(UiDp.dp24))
    Text(
        text = YumeTxt.About.Copyright,
        style = MiuixTheme.textStyles.footnote1,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(UiDp.dp32))
}

/**
 * The floating hero over the list. It owns the version tap counter, so five quick taps hand the
 * screen its one-shot [onShowDebugPanel] signal.
 */
@Composable
private fun AboutHeroLayer(
    coreVersion: String,
    topPadding: Dp,
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
    progress: AboutHeroProgress,
    metrics: AboutHeroMetrics,
    onShowDebugPanel: () -> Unit,
) {
    var versionTapCount by rememberSaveable { mutableIntStateOf(0) }
    AboutHero(
        name = "YumeBox",
        version = "${BuildInfo.VERSION_NAME} · $coreVersion",
        topPadding = topPadding,
        backdrop = backdrop,
        logoBlend = logoBlend,
        progress = progress,
        metrics = metrics,
        onVersionClick = {
            versionTapCount = (versionTapCount + 1).coerceAtMost(5)
            if (versionTapCount == 5) onShowDebugPanel()
        },
    )
}

/** Link cards as data: one row model per entry, rendered by a single [AboutLinkCard]. */
private fun aboutLinkGroups(
    context: Context,
    navigator: Navigator,
    onExportLogs: () -> Unit,
): List<List<AboutLinkRow>> =
    listOf(
        listOf(
            AboutLinkRow.Link(YumeTxt.About.Link.ProjectSource) {
                context.openUrl("https://github.com/YumeYucca/YumeBox")
            },
            AboutLinkRow.Link(YumeTxt.About.Link.CoreSource) {
                context.openUrl("https://github.com/MetaCubeX/mihomo")
            },
        ),
        listOf(
            AboutLinkRow.Link(YumeTxt.About.Link.TelegramGroup) {
                context.openUrl("https://t.me/Eden_Moe")
            },
            AboutLinkRow.Link(YumeTxt.About.Link.TelegramChannel) {
                context.openUrl("https://t.me/YumeYucca")
            },
        ),
        listOf(
            AboutLinkRow.Link(YumeTxt.About.Support.ExportLogs, onClick = onExportLogs),
            AboutLinkRow.Link(YumeTxt.About.Support.ReportIssue) {
                context.openUrl("https://github.com/YumeYucca/YumeBox/issues/new/choose")
            },
        ),
        listOf(
            AboutLinkRow.Link(
                title = YumeTxt.About.License.Libraries,
                summary = YumeTxt.About.License.LibrariesSummary,
            ) {
                navigator.push(Route.OpenSourceLicenses)
            },
            AboutLinkRow.Info(
                title = YumeTxt.About.License.AgplName,
                summary = YumeTxt.About.License.AgplDescription,
            ),
        ),
    )

/** Exports both logs without folding the core's raw output into runtime.log on disk. */
private fun exportStartupLogs(context: Context, targetUri: Uri): Boolean {
    val runtimeLog = RuntimeLog.snapshot(context)
    val coreLog = CoreProcess.coreDiagnosticLog(context)
    val export = buildString {
        appendLine("# YumeBox runtime diagnostics")
        appendLine("# app=${BuildInfo.VERSION_NAME} core=${BuildInfo.CORE_VERSION}")
        appendLine()
        appendLine("# runtime.log")
        append(runtimeLog.ifBlank { "(no runtime entries recorded)\n" })
        if (isNotEmpty() && last() != '\n') appendLine()
        appendLine()
        appendLine("# core.log")
        append(coreLog.ifBlank { "(no core entries recorded)\n" })
    }

    return try {
        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            output.write(export.toByteArray(Charsets.UTF_8))
        } ?: return false
        true
    } catch (error: Exception) {
        // The launcher callback runs on Dispatchers.IO: anything that escapes here would crash
        // the app instead of reporting the failure, so the whole write is guarded.
        Timber.e(error, "Failed to export startup logs")
        false
    }
}

private fun logoBlendColors(isDark: Boolean): List<BlendColorEntry> =
    if (isDark) {
        listOf(
            BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
            BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
            BlendColorEntry(Color(0xff1af500), BlurBlendMode.Lab),
        )
    } else {
        listOf(
            BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
            BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
            BlendColorEntry(Color(0xff1af200), BlurBlendMode.Lab),
        )
    }

private fun cardBlendColors(isDark: Boolean): List<BlendColorEntry> =
    if (isDark) {
        listOf(
            BlendColorEntry(Color(0x4DA9A9A9), BlurBlendMode.Luminosity),
            BlendColorEntry(Color(0x1A9C9C9C), BlurBlendMode.PlusDarker),
        )
    } else {
        listOf(
            BlendColorEntry(Color(0x340034F9), BlurBlendMode.Overlay),
            BlendColorEntry(Color(0xB3FFFFFF), BlurBlendMode.HardLight),
        )
    }

/** One row of an [AboutLinkCard]: a tappable row or a static one. */
private sealed interface AboutLinkRow {
    val title: String
    val summary: String?

    /** Tappable row; what the tap does is the caller's business. */
    class Link(
        override val title: String,
        override val summary: String? = null,
        val onClick: () -> Unit,
    ) : AboutLinkRow

    /** Static row: no chevron and no click. */
    class Info(override val title: String, override val summary: String? = null) : AboutLinkRow
}

/** A glass card holding [rows], followed by the standard gap between cards. */
@Composable
private fun AboutLinkCard(
    backdrop: LayerBackdrop,
    blur: Boolean,
    blendColors: List<BlendColorEntry>,
    rows: List<AboutLinkRow>,
) {
    val cornerRadius = AppTheme.radii.radius24
    AppCard(
        modifier =
            Modifier
                .padding(horizontal = AppTheme.spacing.screenHorizontal)
                .then(
                    if (blur) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(cornerRadius),
                            blurRadius = AboutCardBlurRadius,
                            colors = BlurColors(blendColors = blendColors),
                        )
                    } else {
                        Modifier
                    },
                ),
        cornerRadius = cornerRadius,
        applyHorizontalPadding = false,
        colors =
            CardDefaults.defaultColors(
                color = if (blur) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
            ),
    ) {
        rows.forEach { row ->
            when (row) {
                is AboutLinkRow.Link ->
                    PreferenceArrowItem(
                        title = row.title,
                        summary = row.summary,
                        onClick = row.onClick,
                    )
                is AboutLinkRow.Info ->
                    BasicComponent(title = row.title, summary = row.summary)
            }
        }
    }
    Spacer(Modifier.height(UiDp.dp12))
}
