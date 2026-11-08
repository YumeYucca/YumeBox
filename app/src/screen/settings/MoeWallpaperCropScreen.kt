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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox.screen.settings

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.R
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumeyucca.yumebox.presentation.component.calculateWallpaperViewportLayout
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.Close
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import com.github.yumeyucca.yumebox.presentation.icon.yume.Palette
import com.github.yumeyucca.yumebox.presentation.icon.yume.Zashboard
import com.github.yumeyucca.yumebox.presentation.theme.isDark
import com.github.yumeyucca.yumebox.screen.home.HomeProxyControlState
import com.github.yumeyucca.yumebox.screen.moe.LocalUseSystemWallpaper
import com.github.yumeyucca.yumebox.screen.moe.LocalWallpaperRefreshKey
import com.github.yumeyucca.yumebox.screen.moe.MoeHomeActions
import com.github.yumeyucca.yumebox.screen.moe.MoeHomeLayout
import com.github.yumeyucca.yumebox.screen.moe.MoeHomeLayoutMode
import com.github.yumeyucca.yumebox.screen.moe.MoeHomeLayoutState
import com.github.yumeyucca.yumebox.screen.moe.MoeSidebarIconItem
import com.github.yumeyucca.yumebox.screen.moe.MoeUi
import com.github.yumeyucca.yumebox.screen.moe.SystemWallpaperPreferenceItem
import com.github.yumeyucca.yumebox.screen.moe.formatMoeClock
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.ChevronsLeftRight
import io.github.yumeyucca.lucide.lucide.ListCollapse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.getRoundedCorner

// Palette of the timetable app's customize page: a flat dark scrim around the window, mid-grey
// chips for the tools and a red chip once the tool would clear the picture.
private val EditorScrim = Color(0xFF1A1A1A)
private val EditorToolContainer = Color(0xFF363636)
private val EditorCancelContainer = Color(0xFF5C5C5C)
private val EditorClearContainer = Color(0xFFB71C1C)

private val EditorCapsuleHeight = 40.dp
private val EditorTopBarTopPadding = 16.dp
private val EditorToolButtonSize = 56.dp
private val EditorToolButtonPadding = 7.dp
private val EditorToolRowHeight = EditorToolButtonSize + EditorToolButtonPadding * 2
private val EditorWindowMinCorner = 20.dp

/** Width of the preview window relative to the screen; its height follows the screen's own aspect. */
private const val EditorWindowWidthFraction = 0.7f

/**
 * The home appearance editor. The real home page is laid out at full screen size and scaled down into a window that
 * keeps the screen's aspect, so nothing inside it is squeezed; everything around the window is a flat dark scrim. The
 * tool row below picks a picture (or clears it back to the bundled one) and opens the layout options. Nothing is
 * written back until confirm.
 */
@Composable
fun MoeWallpaperCropScreen(
    navigator: Navigator,
    wallpaperUri: String,
    initialZoom: Float = 1f,
    initialBiasX: Float = 0f,
    initialBiasY: Float = 0f,
) {
    val viewModel = koinViewModel<AppSettingsViewModel>()
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val moe by viewModel.moeHomeSectionState.collectAsState()
    val quoteAuthor by viewModel.moeHomeQuoteAuthor.state.collectAsState()
    val storedWallpaperUri by viewModel.moeWallpaperUri.state.collectAsState()
    val useSystemWallpaper by viewModel.useSystemWallpaper.state.collectAsState()
    val storedStatusBarSpacing by viewModel.moeStatusBarSpacing.state.collectAsState()

    // A blank source previews the bundled wallpaper, exactly as the home page falls back to it.
    var wallpaperSource by remember { mutableStateOf(wallpaperUri) }
    var followSystemWallpaper by remember { mutableStateOf(useSystemWallpaper) }
    var zoom by remember { mutableFloatStateOf(initialZoom.coerceIn(1f, 5f)) }
    var biasX by remember { mutableFloatStateOf(initialBiasX.coerceIn(-1f, 1f)) }
    var biasY by remember { mutableFloatStateOf(initialBiasY.coerceIn(-1f, 1f)) }
    var sidebarExpanded by remember { mutableStateOf(moe.sidebarExpanded) }
    var statusBarSpacing by remember { mutableStateOf(storedStatusBarSpacing) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var applying by remember { mutableStateOf(false) }

    val sourceChanged = wallpaperSource != storedWallpaperUri
    val previewUsesSystemWallpaper = followSystemWallpaper
    val hasCustomWallpaper = !followSystemWallpaper && wallpaperSource.isNotBlank()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val imageBounds by
        produceState<Pair<Int, Int>?>(initialValue = null, wallpaperSource, previewUsesSystemWallpaper) {
            value =
                if (previewUsesSystemWallpaper) {
                    null
                } else {
                    withContext(Dispatchers.IO) { readWallpaperBounds(context, wallpaperSource) }
                }
        }
    val wallpaperPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            wallpaperSource = uri.toString()
            followSystemWallpaper = false
            zoom = 1f
            biasX = 0f
            biasY = 0f
        }

    Scaffold { _ ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(EditorScrim)) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val topReserved = statusBarInset + EditorTopBarTopPadding + EditorCapsuleHeight + 16.dp
            val bottomReserved = navigationBarInset + EditorToolRowHeight + 16.dp

            // Width-driven like the reference, shrunk only when a short screen cannot fit the chrome.
            val screenAspect = screenHeight / screenWidth
            val availableHeight = (screenHeight - topReserved - bottomReserved).coerceAtLeast(1.dp)
            val fullWindowHeight = screenWidth * EditorWindowWidthFraction * screenAspect
            val windowHeight = minOf(fullWindowHeight, availableHeight)
            val windowWidth = windowHeight / screenAspect
            val previewScale = windowWidth / screenWidth
            val windowTop =
                ((screenHeight - windowHeight) / 2)
                    .coerceAtMost(screenHeight - bottomReserved - windowHeight)
                    .coerceAtLeast(topReserved)
            val windowCorner = maxOf(getRoundedCorner() * previewScale, EditorWindowMinCorner)
            val windowShape = RoundedCornerShape(windowCorner)

            // The viewport is solved at full screen size, the size the preview is actually laid out at.
            val viewportLayout =
                calculateWallpaperViewportLayout(
                    containerWidthPx = with(density) { screenWidth.toPx() }.coerceAtLeast(1f),
                    containerHeightPx = with(density) { screenHeight.toPx() }.coerceAtLeast(1f),
                    imageWidthPx = imageBounds?.first?.toFloat(),
                    imageHeightPx = imageBounds?.second?.toFloat(),
                    zoom = zoom,
                    biasX = biasX,
                    biasY = biasY,
                )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = windowTop)
                        .size(width = windowWidth, height = windowHeight)
                        .clip(windowShape),
            ) {
                Box(
                    modifier =
                        Modifier
                            .wrapContentSize(align = Alignment.Center, unbounded = true)
                            .requiredSize(width = screenWidth, height = screenHeight)
                            .graphicsLayer {
                                scaleX = previewScale
                                scaleY = previewScale
                            },
                ) {
                    CompositionLocalProvider(LocalUseSystemWallpaper provides previewUsesSystemWallpaper) {
                        HomePreview(
                            wallpaperUri = wallpaperSource,
                            zoom = zoom,
                            biasX = viewportLayout.biasX,
                            biasY = viewportLayout.biasY,
                            sidebarExpanded = sidebarExpanded,
                            quote = moe.moeHomeQuote.ifBlank { YumeTxt.AppSettings.Interface.HomeQuoteDefault },
                            quoteAuthor = quoteAuthor,
                            isDarkSurface = moe.themeMode.isDark(),
                            now = now,
                            statusBarTop =
                                if (statusBarSpacing) statusBarInset else MoeUi.Hero.containerHorizontalInset,
                        )
                    }
                }
                // Covers the preview so it only pans the picture; the home's own taps stay inert here.
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .pointerInput(viewportLayout.maxShiftX, viewportLayout.maxShiftY, previewScale) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Window pixels are scaled-down screen pixels.
                                    biasX = panBias(biasX, dragAmount.x / previewScale, viewportLayout.maxShiftX)
                                    biasY = panBias(biasY, dragAmount.y / previewScale, viewportLayout.maxShiftY)
                                }
                            },
                )
            }

            WallpaperEditorTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                sidebarExpanded = sidebarExpanded,
                onCancel = { navigator.pop() },
                onSidebarExpandedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    sidebarExpanded = it
                },
                onConfirm = {
                    if (applying) return@WallpaperEditorTopBar
                    applying = true
                    val savedZoom = zoom
                    val savedBiasX = viewportLayout.biasX
                    val savedBiasY = viewportLayout.biasY
                    val saveCrop = { viewModel.onMoeWallpaperCropChange(savedZoom, savedBiasX, savedBiasY) }
                    viewModel.onMoeSidebarExpandedChange(sidebarExpanded)
                    viewModel.onMoeStatusBarSpacingChange(statusBarSpacing)
                    when {
                        // Following the system wallpaper leaves the stored picture untouched.
                        followSystemWallpaper -> {
                            viewModel.onUseSystemWallpaperChange(true)
                            saveCrop()
                            navigator.pop()
                        }

                        !sourceChanged -> {
                            viewModel.onUseSystemWallpaperChange(false)
                            saveCrop()
                            navigator.pop()
                        }

                        wallpaperSource.isBlank() -> {
                            viewModel.onUseSystemWallpaperChange(false)
                            viewModel.clearMoeWallpaperUri {
                                saveCrop()
                                navigator.pop()
                            }
                        }

                        else -> {
                            saveCrop()
                            viewModel.applyMoeWallpaper(sourceUri = wallpaperSource) { navigator.pop() }
                        }
                    }
                },
            )

            WallpaperEditorToolRow(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(screenHeight - windowTop - windowHeight)
                        .navigationBarsPadding(),
                hasCustomWallpaper = hasCustomWallpaper,
                onPickWallpaper = {
                    wallpaperPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onClearWallpaper = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    wallpaperSource = ""
                    zoom = 1f
                    biasX = 0f
                    biasY = 0f
                },
                onCustomize = { showCustomizeSheet = true },
            )
        }

        AppActionBottomSheet(
            show = showCustomizeSheet,
            title = YumeTxt.Home.Settings.Title,
            onDismissRequest = { showCustomizeSheet = false },
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    applyHorizontalPadding = false,
                ) {
                    SystemWallpaperPreferenceItem(
                        checked = followSystemWallpaper,
                        onCheckedChange = { followSystemWallpaper = it },
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.Home.Settings.StatusBarSpacing,
                        checked = statusBarSpacing,
                        onCheckedChange = { statusBarSpacing = it },
                    )
                }
            }
        }
    }
}

private fun panBias(bias: Float, delta: Float, maxShift: Float): Float {
    if (maxShift <= 0.5f) return 0f
    val next = (bias.coerceIn(-1f, 1f) * maxShift - delta).coerceIn(-maxShift, maxShift)
    return (next / maxShift).coerceIn(-1f, 1f)
}

/** Reads the picture's pixel size; an unreadable or blank source measures the bundled wallpaper it falls back to. */
private fun readWallpaperBounds(context: Context, source: String): Pair<Int, Int>? {
    fun decode(open: () -> java.io.InputStream?): Pair<Int, Int>? =
        runCatching {
            open()?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
            }
        }.getOrNull()

    val custom = if (source.isBlank()) null else decode { context.contentResolver.openInputStream(source.toUri()) }
    return custom ?: decode { context.resources.openRawResource(R.drawable.wallpaper) }
}

/**
 * The real home layout rendered non-interactively at full screen size, so the preview cannot drift from the page it
 * edits. It keeps the live clock and the draft wallpaper/rail state; the actions are no-ops because this is not the
 * home.
 */
@Composable
private fun HomePreview(
    wallpaperUri: String,
    zoom: Float,
    biasX: Float,
    biasY: Float,
    sidebarExpanded: Boolean,
    quote: String,
    quoteAuthor: String,
    isDarkSurface: Boolean,
    now: Long,
    statusBarTop: Dp,
) {
    val sidebarToggleProgress by
        animateFloatAsState(
            targetValue = if (sidebarExpanded) 1f else 0f,
            animationSpec = tween(durationMillis = 320),
            label = "wallpaper_sidebar_preview",
        )
    val settledProgress = remember { mutableFloatStateOf(1f) }
    val icons =
        remember {
            listOf(
                MoeSidebarIconItem(Yume.Zashboard) {},
                MoeSidebarIconItem(Yume.PackageCheck) {},
                MoeSidebarIconItem(Yume.Bolt) {},
                MoeSidebarIconItem(Yume.Palette) {},
            )
        }
    val actions =
        remember {
            MoeHomeActions(
                toggleSidebar = {},
                pickWallpaper = {},
                openSettings = {},
                toggleProxy = {},
                openUpdate = {},
            )
        }
    val state =
        MoeHomeLayoutState(
            wallpaperUri = wallpaperUri,
            wallpaperZoom = zoom,
            wallpaperBiasX = biasX,
            wallpaperBiasY = biasY,
            statusBarTop = statusBarTop,
            pageProgress = settledProgress,
            sidebarProgress = settledProgress,
            sidebarToggleProgress = sidebarToggleProgress,
            duration = formatMoeClock(now),
            batteryPercent = null,
            sidebarIcons = icons,
            contentSurface = if (isDarkSurface) MiuixTheme.colorScheme.surface else Color.White,
            isRunning = false,
            traffic = TrafficData.zero,
            selectedServerName = null,
            selectedServerPing = null,
            now = now,
            quote = quote,
            quoteAuthor = quoteAuthor,
            controlState = HomeProxyControlState.Idle,
            canLaunch = false,
            isRemoteController = false,
            hasUpdate = false,
        )

    CompositionLocalProvider(LocalWallpaperRefreshKey provides 0) {
        with(actions) { MoeHomeLayout(state, MoeHomeLayoutMode.Phone) }
    }
}

@Composable
private fun WallpaperEditorTopBar(
    modifier: Modifier = Modifier,
    sidebarExpanded: Boolean,
    onCancel: () -> Unit,
    onSidebarExpandedChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = EditorTopBarTopPadding, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCapsuleButton(
            label = YumeTxt.Component.Button.Cancel,
            container = EditorCancelContainer,
            onClick = onCancel,
        )
        SidebarPreviewSwitch(
            expanded = sidebarExpanded,
            onExpandedChange = onSidebarExpandedChange,
        )
        EditorCapsuleButton(
            label = YumeTxt.Component.Button.Confirm,
            container = MiuixTheme.colorScheme.primary,
            onClick = onConfirm,
        )
    }
}

@Composable
private fun EditorCapsuleButton(
    label: String,
    container: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(84.dp)
                .height(EditorCapsuleHeight)
                .clip(CircleShape)
                .background(container)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SidebarPreviewSwitch(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .width(120.dp)
                .height(EditorCapsuleHeight)
                .clip(CircleShape)
                .background(EditorToolContainer)
                .padding(3.dp),
    ) {
        val halfWidth = maxWidth / 2
        val thumbOffset by
            animateDpAsState(
                targetValue = if (expanded) halfWidth else 0.dp,
                animationSpec = tween(durationMillis = 280),
                label = "wallpaper_sidebar_thumb",
            )
        Box(
            modifier =
                Modifier
                    .offset(x = thumbOffset)
                    .width(halfWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            SidebarPreviewOption(
                icon = Lucide.ListCollapse,
                selected = !expanded,
                modifier = Modifier.weight(1f),
                onClick = { onExpandedChange(false) },
            )
            SidebarPreviewOption(
                icon = Lucide.ChevronsLeftRight,
                selected = expanded,
                modifier = Modifier.weight(1f),
                onClick = { onExpandedChange(true) },
            )
        }
    }
}

@Composable
private fun SidebarPreviewOption(
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Wallpaper chip | divider | customize chip, centred in the gap below the window. */
@Composable
private fun WallpaperEditorToolRow(
    modifier: Modifier = Modifier,
    hasCustomWallpaper: Boolean,
    onPickWallpaper: () -> Unit,
    onClearWallpaper: () -> Unit,
    onCustomize: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EditorToolButton(
                icon = if (hasCustomWallpaper) Yume.Close else Yume.Palette,
                contentDescription =
                    if (hasCustomWallpaper) YumeTxt.Component.Button.Clear else YumeTxt.Home.Settings.ChangeWallpaper,
                container = if (hasCustomWallpaper) EditorClearContainer else EditorToolContainer,
                onClick = if (hasCustomWallpaper) onClearWallpaper else onPickWallpaper,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier =
                    Modifier
                        .padding(EditorToolButtonPadding)
                        .width(2.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(EditorToolContainer),
            )
            Spacer(modifier = Modifier.width(4.dp))
            EditorToolButton(
                icon = Yume.Edit,
                contentDescription = YumeTxt.Home.Settings.Title,
                container = EditorToolContainer,
                onClick = onCustomize,
            )
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(EditorToolButtonPadding)
                .size(EditorToolButtonSize)
                .clip(CircleShape)
                .background(container)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}
