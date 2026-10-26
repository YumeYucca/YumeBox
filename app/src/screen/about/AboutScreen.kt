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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.common.util.openUrl
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.IOException
import org.koin.compose.koinInject

@Composable
fun AboutScreen(navigator: Navigator) {
    val context = LocalContext.current
    val appSettingsStore = koinInject<AppSettingsStore>()
    val scope = rememberCoroutineScope()
    var myBooksTapCount by rememberSaveable { mutableIntStateOf(0) }
    var showDebugPanel by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val exportLogsLauncher =
        rememberLauncherForActivityResult(
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
    val coreVersion = BuildInfo.CORE_VERSION
    val backdrop = rememberLayerBackdrop()
    val blur = remember { isRuntimeShaderSupported() }
    val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val logoBlend =
        remember(isDark) {
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
        }
    val cardBlend =
        remember(isDark) {
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
        }
    var heroHeight by remember { mutableStateOf(200.dp) }
    var spacerBottom by remember { mutableFloatStateOf(0f) }
    var iconBottom by remember { mutableFloatStateOf(0f) }
    var nameBottom by remember { mutableFloatStateOf(0f) }
    var versionBottom by remember { mutableFloatStateOf(0f) }
    var initialSpacerBottom by remember { mutableFloatStateOf(0f) }
    var iconProgress by remember { mutableFloatStateOf(0f) }
    var nameProgress by remember { mutableFloatStateOf(0f) }
    var versionProgress by remember { mutableFloatStateOf(0f) }
    var spacerHeightPx by remember { mutableIntStateOf(0) }
    val spacerHeight = heroHeight + 230.dp
    val scrollProgress by remember(listState) {
        derivedStateOf {
            if (spacerHeightPx <= 0) 0f
            else if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset.toFloat() / spacerHeightPx).coerceIn(0f, 1f)
        }
    }
    val visible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .onEach { (index, offset) ->
                if (index > 0) {
                    iconProgress = 1f
                    nameProgress = 1f
                    versionProgress = 1f
                    return@onEach
                }
                if (offset == 0) {
                    iconProgress = 0f
                    nameProgress = 0f
                    versionProgress = 0f
                    return@onEach
                }
                if (initialSpacerBottom == 0f && spacerBottom > 0f) {
                    initialSpacerBottom = spacerBottom
                }
                val logoAreaBottom =
                    if (initialSpacerBottom > 0f) initialSpacerBottom else spacerBottom
                val versionStage = logoAreaBottom - versionBottom
                val nameStage = versionBottom - nameBottom
                val iconStage = nameBottom - iconBottom
                val versionDelay = versionStage * 0.5f

                versionProgress =
                    ((offset - versionDelay) / (versionStage - versionDelay).coerceAtLeast(1f))
                        .coerceIn(0f, 1f)
                nameProgress =
                    ((offset - versionStage) / nameStage.coerceAtLeast(1f)).coerceIn(0f, 1f)
                iconProgress =
                    ((offset - versionStage - nameStage) / iconStage.coerceAtLeast(1f))
                        .coerceIn(0f, 1f)
            }
            .collect {}
    }
    val titleAlpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)

    GlassScaffold(
        topBar = {
            val surface = MiuixTheme.colorScheme.surface
            val barBackdrop = LocalTopBarBackdrop.current?.takeIf { blur && scrollProgress == 1f }
            val barColor = if (scrollProgress == 1f && barBackdrop == null) surface else Color.Transparent
            Box(
                modifier =
                    if (barBackdrop != null) {
                        Modifier.textureBlur(
                            backdrop = barBackdrop,
                            shape = RectangleShape,
                            blurRadius = 25f,
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
        },
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        val contentMinHeight by remember(density, listState) {
            derivedStateOf {
                val info = listState.layoutInfo
                val available =
                    (info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding)
                        .coerceAtLeast(0)
                with(density) { available.toDp() }
            }
        }
        Box(Modifier.fillMaxSize()) {
            BgEffectBackground(
                dynamicBackground = visible,
                modifier = Modifier.fillMaxSize(),
                bgModifier = if (blur) Modifier.layerBackdrop(backdrop) else Modifier,
                isFullSize = true,
                alpha = { 1f - scrollProgress },
            ) {}
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
                lazyListState = listState,
            ) {
                item(key = "logoSpacer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(spacerHeight)
                            .onSizeChanged { spacerHeightPx = it.height }
                            .onGloballyPositioned { coords ->
                                spacerBottom = coords.positionInWindow().y + coords.size.height
                            },
                    )
                }
                item(key = "aboutContent") {
                    Column(Modifier.fillMaxWidth().heightIn(min = contentMinHeight)) {
                        AnimatedVisibility(
                            visible = showDebugPanel,
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

                        AboutGlassCard(backdrop, blur, cardBlend) {
                            ArrowPreference(
                                title = YumeTxt.About.Link.ProjectSource,
                                onClick = { openUrl(context, "https://github.com/YumeYucca/YumeBox") },
                            )
                            ArrowPreference(
                                title = YumeTxt.About.Link.CoreSource,
                                onClick = { openUrl(context, "https://github.com/MetaCubeX/mihomo") },
                            )
                        }
                        Spacer(Modifier.height(UiDp.dp12))

                        AboutGlassCard(backdrop, blur, cardBlend) {
                            ArrowPreference(
                                title = YumeTxt.About.Link.TelegramGroup,
                                onClick = { openUrl(context, "https://t.me/Eden_Moe") },
                            )
                            ArrowPreference(
                                title = YumeTxt.About.Link.TelegramChannel,
                                onClick = { openUrl(context, "https://t.me/YumeYucca") },
                            )
                        }
                        Spacer(Modifier.height(UiDp.dp12))

                        AboutGlassCard(backdrop, blur, cardBlend) {
                            ArrowPreference(
                                title = YumeTxt.About.Support.ExportLogs,
                                onClick = {
                                    exportLogsLauncher.launch(
                                        "yumebox_diagnostics_${System.currentTimeMillis()}.log"
                                    )
                                },
                            )
                            ArrowPreference(
                                title = YumeTxt.About.Support.ReportIssue,
                                onClick = {
                                    openUrl(context, "https://github.com/YumeYucca/YumeBox/issues/new/choose")
                                },
                            )
                        }
                        Spacer(Modifier.height(UiDp.dp12))

                        AboutGlassCard(backdrop, blur, cardBlend) {
                            ArrowPreference(
                                title = YumeTxt.About.License.Libraries,
                                summary = YumeTxt.About.License.LibrariesSummary,
                                onClick = { navigator.push(Route.OpenSourceLicenses) },
                            )
                            BasicComponent(
                                title = YumeTxt.About.License.AgplName,
                                summary = YumeTxt.About.License.AgplDescription,
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = UiDp.dp32),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = YumeTxt.About.Copyright, style = MiuixTheme.textStyles.footnote1)
                    }
                    Spacer(modifier = Modifier.height(UiDp.dp32))
                }
            }
            AboutHero(
                name = "YumeBox",
                version = "${BuildInfo.VERSION_NAME} · $coreVersion",
                topPadding = innerPadding.calculateTopPadding(),
                backdrop = if (blur) backdrop else null,
                logoBlend = logoBlend,
                versionProgress = versionProgress,
                nameProgress = nameProgress,
                iconProgress = iconProgress,
                onHeight = { if (heroHeight != it) heroHeight = it },
                onIconBottom = {
                    if (iconBottom == 0f) iconBottom = it
                },
                onNameBottom = {
                    if (nameBottom == 0f) nameBottom = it
                },
                onVersionBottom = {
                    if (versionBottom == 0f) versionBottom = it
                },
                onVersionClick = {
                    myBooksTapCount = (myBooksTapCount + 1).coerceAtMost(5)
                    if (myBooksTapCount == 5) showDebugPanel = true
                },
            )
        }
    }
}

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
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
private fun AboutGlassCard(
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop,
    blur: Boolean,
    blendColors: List<BlendColorEntry>,
    content: @Composable () -> Unit,
) {
    AppCard(
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .then(
                    if (blur) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = RoundedCornerShape(24.dp),
                            blurRadius = 60f,
                            colors = BlurColors(blendColors = blendColors),
                        )
                    } else {
                        Modifier
                    },
                ),
        cornerRadius = 24,
        applyHorizontalPadding = false,
        colors =
            CardDefaults.defaultColors(
                color = if (blur) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer,
            ),
    ) {
        content()
    }
}
