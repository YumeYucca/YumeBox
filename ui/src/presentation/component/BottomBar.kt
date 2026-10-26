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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowDownUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.House
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.YumeBlur.glassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val BottomBarLayoutAnimationDurationMillis = 380
private const val ProxyDestinationRevealDurationMillis = 300
private const val PagerAnimateScrollPreJumpThreshold = 3

class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        val boundedTarget = targetIndex.coerceIn(0, pagerState.pageCount - 1)
        if (boundedTarget == selectedPage && (isNavigating || !pagerState.isScrollInProgress)) return

        navJob?.cancel()
        selectedPage = boundedTarget
        isNavigating = true
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                val hops = abs(boundedTarget - pagerState.currentPage)
                val animationSpec = MainBottomBarDefaults.pagerNavigationAnimationSpec(hops)
                if (
                    hops >= PagerAnimateScrollPreJumpThreshold &&
                        pagerState.layoutInfo.pageSize > 0
                ) {
                    pagerState.animateScrollToPageUninterrupted(
                        page = boundedTarget,
                        animationSpec = animationSpec,
                    )
                } else {
                    pagerState.animateScrollToPage(
                        page = boundedTarget,
                        animationSpec = animationSpec,
                    )
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != boundedTarget) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

private suspend fun PagerState.animateScrollToPageUninterrupted(
    page: Int,
    animationSpec: AnimationSpec<Float>,
) {
    val target = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val pageSizeWithSpacing = layoutInfo.pageSize + layoutInfo.pageSpacing
    if (pageSizeWithSpacing <= 0) {
        animateScrollToPage(page = target, animationSpec = animationSpec)
        return
    }
    scroll {
        updateTargetPage(target)
        val distance = (target - currentPage - currentPageOffsetFraction) * pageSizeWithSpacing
        var previous = 0f
        animate(0f, distance, animationSpec = animationSpec) { value, _ ->
            previous += scrollBy(value - previous)
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState =
    remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }

val LocalPagerState = compositionLocalOf<PagerState> { error("LocalPagerState is not provided") }
val LocalMainPagerState =
    compositionLocalOf<MainPagerState> { error("LocalMainPagerState is not provided") }
val LocalHandlePageChange =
    compositionLocalOf<(BottomBarDestination) -> Unit> {
        error("LocalHandlePageChange is not provided")
    }
val LocalNavigator = compositionLocalOf<Navigator> { error("LocalNavigator is not provided") }

/**
 * Right-pane navigator for the tablet dual-pane shell.
 *
 * Non-null only when [WindowLayoutMode.usesSplitShell] is active. Treat non-null as the single
 * source of truth for "we are in split shell" — do not re-derive shell mode from width in modules.
 */
val LocalDetailNavigator = compositionLocalOf<Navigator?> { null }

/** True when the composition is hosted inside the tablet dual-pane shell. */
inline val Navigator?.isSplitShell: Boolean
    get() = this != null

object MainBottomBarDefaults {
    // NexioSchedule-aligned capsule sizing: each tab owns 21% of the screen width, so the pill
    // centers itself at 0.63 for three tabs and grows to 0.84 when the proxy tab is revealed.
    const val WidthFractionPerTab = 0.21f

    // Two-tab floor: 0.21 x 2 reads too narrow (user feedback) — 0.46 keeps a touch more
    // per-tab room than the three-tab layout (0.21/tab) without swallowing the screen.
    // Tuning trail: 0.42 too narrow, 0.5 too wide, 0.46 approved direction.
    const val MinBarWidthFraction = 0.46f

    // NexioSchedule-aligned floating offset: hug the gesture-nav inset with 2dp of breathing
    // room, but never sit closer than 22dp to the screen bottom.
    val MinFloatingBottomGap = UiDp.dp22
    val InsetBreathingRoom = UiDp.dp2
    val ExitOffset = UiDp.dp84
    val BarHeight = UiDp.dp56
    private const val PagerNavigationDurationMillis = 360
    val PagerAnimationSpec: AnimationSpec<Float> =
        spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Int.VisibilityThreshold.toFloat(),
        )

    fun pagerNavigationAnimationSpec(pageHops: Int): AnimationSpec<Float> {
        val hops = pageHops.coerceAtLeast(1)
        val durationMillis =
            if (hops <= 2) {
                PagerNavigationDurationMillis
            } else {
                PagerNavigationDurationMillis * hops / 2
            }
        return tween(durationMillis = durationMillis, easing = AnimationSpecs.Legacy)
    }
}

@Composable
fun rememberMainPagerFlingBehavior(pagerState: PagerState): TargetedFlingBehavior =
    PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = MainBottomBarDefaults.PagerAnimationSpec,
    )

@Composable
fun rememberBottomBarReservedHeight(): Dp {
    val density = LocalDensity.current
    val systemBottomInset =
        with(density) {
            max(
                WindowInsets.navigationBars.getBottom(this),
                WindowInsets.systemGestures.getBottom(this),
            )
                .toDp()
        }
    return remember(systemBottomInset) {
        MainBottomBarDefaults.BarHeight +
            maxOf(
                systemBottomInset + MainBottomBarDefaults.InsetBreathingRoom,
                MainBottomBarDefaults.MinFloatingBottomGap,
            )
    }
}

@Composable
fun BottomBarContent(
    isVisible: Boolean = true,
    destinations: List<BottomBarDestination> = BottomBarDestination.entries,
) {
    FloatingBottomBarContent(isVisible = isVisible, destinations = destinations)
}

@Composable
private fun FloatingBottomBarContent(
    isVisible: Boolean = true,
    destinations: List<BottomBarDestination> = BottomBarDestination.entries,
) {
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val mainPagerState = LocalMainPagerState.current
    val pagerState = mainPagerState.pagerState
    // Selection and indicator must share the pager's physical position. selectedPage is updated
    // optimistically before navigation starts, which previously recolored an icon while the
    // indicator was still parked on the old page.
    val page by remember(pagerState) { derivedStateOf { pagerState.currentPage } }
    // Tabs exclude Home: the home page owns its chrome and the bar is hidden there anyway, so the
    // pager's page 0 has no tab. Tab index = pager page index - 1 throughout the math below.
    val tabs = remember(destinations) { destinations.filter { it != BottomBarDestination.Home } }
    val indicatorProgress by
    remember(pagerState, tabs.size) {
        derivedStateOf {
            (
                pagerState.currentPage.toFloat() +
                    pagerState.currentPageOffsetFraction - 1f
                ).coerceIn(0f, (tabs.size - 1).toFloat())
        }
    }
    val bottomBarVisible = isVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    var revealProxyDestination by remember { mutableStateOf(false) }
    LaunchedEffect(BottomBarDestination.Proxy in tabs) {
        revealProxyDestination = BottomBarDestination.Proxy in tabs
    }
    val density = LocalDensity.current
    val opacity = AppTheme.opacity
    val exitOffsetPx =
        remember(density) { with(density) { MainBottomBarDefaults.ExitOffset.toPx() } }
    val animatedTranslationY = remember { Animatable(if (bottomBarVisible) 0f else exitOffsetPx) }
    val animatedAlpha by
    animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 180,
                easing =
                    if (bottomBarVisible) {
                        AnimationSpecs.EmphasizedDecelerate
                    } else {
                        AnimationSpecs.EmphasizedAccelerate
                    },
            ),
        label = "legacy_bottom_bar_alpha",
    )

    LaunchedEffect(bottomBarVisible, exitOffsetPx) {
        if (bottomBarVisible) {
            animatedTranslationY.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(durationMillis = 280, easing = AnimationSpecs.EmphasizedDecelerate),
            )
        } else {
            animatedTranslationY.animateTo(
                targetValue = exitOffsetPx,
                animationSpec =
                    tween(durationMillis = 220, easing = AnimationSpecs.EmphasizedAccelerate),
            )
        }
    }

    val handlePageChange = LocalHandlePageChange.current
    val onItemClick: (BottomBarDestination) -> Unit = { destination ->
        val index = destinations.indexOf(destination)
        if (index != mainPagerState.selectedPage ||
            (!mainPagerState.isNavigating && pagerState.currentPageOffsetFraction != 0f)) {
            handlePageChange(destination)
        }
    }

    val bottomSafeInset =
        with(density) {
            val navigationBottom = WindowInsets.navigationBars.getBottom(this)
            val gestureBottom = WindowInsets.systemGestures.getBottom(this)
            max(navigationBottom, gestureBottom).toDp()
        }
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val selectedColor = MiuixTheme.colorScheme.primary
    val unselectedColor = MiuixTheme.colorScheme.onSurface.copy(alpha = opacity.secondaryText)
    // NexioSchedule-style frosted glass: with a live glass backdrop the capsule frosts the page
    // behind it under a translucent surfaceContainer tint; without one (setting off or no
    // RuntimeShader) it falls back to a solid capsule, never raw see-through.
    val glassBackdrop = LocalGlassBackdrop.current
    val fallbackColor = MiuixTheme.colorScheme.surfaceContainer
    val indicatorContainerColor = selectedColor.copy(alpha = opacity.subtle)
    val widthFraction by
        animateFloatAsState(
            targetValue =
                (MainBottomBarDefaults.WidthFractionPerTab * tabs.size)
                    .coerceIn(MainBottomBarDefaults.MinBarWidthFraction, 1f),
            animationSpec =
                tween(
                    durationMillis = BottomBarLayoutAnimationDurationMillis,
                    easing = AnimationSpecs.EmphasizedDecelerate,
                ),
            label = "bottom_bar_width_fraction",
        )

    if (glassBackdrop != null && isRuntimeShaderSupported()) {
        if (tabs.isEmpty()) return
        GlassNavigationBar(
            items = tabs.map { GlassNavigationItem(icon = it.icon, label = it.label) },
            selectedIndex = ((if (mainPagerState.isNavigating) mainPagerState.selectedPage else page) - 1)
                .coerceIn(0, tabs.lastIndex),
            onSelect = { index -> tabs.getOrNull(index)?.let(onItemClick) },
            backdrop = glassBackdrop,
            height = MainBottomBarDefaults.BarHeight,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            indicatorColor = indicatorContainerColor,
            indicatorPressedColor = selectedColor.copy(alpha = opacity.subtle * 2f),
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .padding(bottom = maxOf(
                    bottomSafeInset + MainBottomBarDefaults.InsetBreathingRoom,
                    MainBottomBarDefaults.MinFloatingBottomGap,
                ))
                .graphicsLayer {
                    alpha = animatedAlpha
                    translationY = animatedTranslationY.value
                },
        )
        return
    }

    LegacyBottomNavigationBar(
        indicatorProgress = indicatorProgress,
        tabsCount = tabs.size,
        isLightTheme = isLightTheme,
        glassBackdrop = glassBackdrop,
        fallbackColor = fallbackColor,
        indicatorContainerColor = indicatorContainerColor,
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .padding(
                    bottom =
                        maxOf(
                            bottomSafeInset + MainBottomBarDefaults.InsetBreathingRoom,
                            MainBottomBarDefaults.MinFloatingBottomGap,
                        )
                )
                .graphicsLayer {
                    alpha = animatedAlpha
                    translationY = animatedTranslationY.value
                },
    ) { lookaheadScope ->
        tabs.forEach { destination ->
            val proxyItemAlpha by
            animateFloatAsState(
                targetValue =
                    if (destination != BottomBarDestination.Proxy || revealProxyDestination) 1f
                    else 0f,
                animationSpec =
                    tween(
                        durationMillis = ProxyDestinationRevealDurationMillis,
                        easing = AnimationSpecs.EmphasizedDecelerate,
                    ),
                label = "bottom_bar_proxy_item_alpha",
            )
            BottomBarTab(
                destination = destination,
                selected = tabs.getOrNull(page - 1) == destination,
                enabled = bottomBarVisible,
                selectedColor = selectedColor,
                unselectedColor = unselectedColor,
                onClick = { onItemClick(destination) },
                modifier =
                    Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = proxyItemAlpha
                            scaleX =
                                if (destination == BottomBarDestination.Proxy)
                                    0.82f + proxyItemAlpha * 0.18f
                                else 1f
                            scaleY =
                                if (destination == BottomBarDestination.Proxy)
                                    0.82f + proxyItemAlpha * 0.18f
                                else 1f
                        }
                        .animateBounds(
                            lookaheadScope = lookaheadScope,
                            boundsTransform = { _, _ ->
                                tween(
                                    durationMillis = BottomBarLayoutAnimationDurationMillis,
                                    easing = AnimationSpecs.EmphasizedDecelerate,
                                )
                            },
                        ),
            )
        }
    }
}

@Composable
private fun BottomBarTab(
    destination: BottomBarDestination,
    selected: Boolean,
    enabled: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val itemColor = if (selected) selectedColor else unselectedColor
    LegacyBottomNavigationTabItem(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.size(UiDp.dp24), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = itemColor,
            )
        }
        BasicText(
            text = destination.label,
            style =
                TextStyle(
                    color = itemColor,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
        )
    }
}

@Composable
private fun LegacyBottomNavigationBar(
    indicatorProgress: Float,
    tabsCount: Int,
    isLightTheme: Boolean,
    glassBackdrop: LayerBackdrop?,
    fallbackColor: Color,
    indicatorContainerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(LookaheadScope) -> Unit,
) {
    val opacity = AppTheme.opacity
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val surfaceWidthPx = remember { mutableIntStateOf(0) }
    val safeIndicatorProgress = indicatorProgress.coerceIn(0f, (tabsCount - 1).toFloat())
    val contentInsetPx = with(density) { (UiDp.dp4 * 2).toPx() }
    val innerWidthPx = (surfaceWidthPx.intValue - contentInsetPx).coerceAtLeast(0f)
    val tabWidthPx = if (tabsCount > 0) innerWidthPx / tabsCount else 0f
    val indicatorOffsetPx =
        if (isLtr) {
            safeIndicatorProgress * tabWidthPx
        } else {
            innerWidthPx - (safeIndicatorProgress + 1f) * tabWidthPx
        }
    val borderShadowColor =
        if (isLightTheme) {
            // The translucent capsule on a near-white page needs the drop shadow to carry the
            // lift; the additive edge-light rim alone cannot separate it there.
            Black.copy(alpha = opacity.softOverlay)
        } else {
            Black.copy(alpha = opacity.surfaceSoft)
        }

    Box(
        modifier =
            modifier
                .onSizeChanged { surfaceWidthPx.intValue = it.width }
                .graphicsLayer {
                    shape = Capsule()
                    clip = false
                    shadowElevation = with(density) { UiDp.dp7.toPx() }
                    ambientShadowColor = borderShadowColor
                    spotShadowColor = borderShadowColor
                }
                .height(MainBottomBarDefaults.BarHeight)
                .clip(Capsule())
                .then(
                    Modifier.glassSurface(
                        backdrop = glassBackdrop,
                        shape = Capsule(),
                        fallbackColor = fallbackColor,
                        tint =
                            MiuixTheme.colorScheme.surfaceContainer.copy(
                                alpha = if (isLightTheme) 0.5f else 0.6f,
                            ),
                    )
                )
                .edgeLight(
                    shape = Capsule(),
                    color =
                        EdgeLightDefaults.tint(
                            MiuixTheme.colorScheme.surfaceContainer,
                            isLightTheme,
                        ),
                    width = EdgeLightDefaults.width(isLightTheme),
                    blurRadius = EdgeLightDefaults.BlurRadius,
                    blendMode = EdgeLightDefaults.blendMode(isLightTheme),
                ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (tabWidthPx > 0f) {
            LegacyBottomNavigationIndicator(
                modifier = Modifier
                    .padding(UiDp.dp4)
                    .align(Alignment.CenterStart),
                indicatorOffsetPx = indicatorOffsetPx,
                indicatorWidthPx = tabWidthPx,
                indicatorContainerColor = indicatorContainerColor,
            )
        }

        LookaheadScope {
            Row(
                modifier =
                    Modifier
                        .padding(UiDp.dp4)
                        .height(UiDp.dp48)
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content(this@LookaheadScope)
            }
        }
    }
}

@Composable
private fun LegacyBottomNavigationIndicator(
    modifier: Modifier = Modifier,
    indicatorOffsetPx: Float,
    indicatorWidthPx: Float,
    indicatorContainerColor: Color,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            modifier
                .offset { IntOffset(indicatorOffsetPx.roundToInt(), 0) }
                .width(with(density) { indicatorWidthPx.toDp() })
                .height(UiDp.dp48)
                .background(indicatorContainerColor, Capsule())
    )
}

@Composable
private fun LegacyBottomNavigationTabItem(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(Capsule())
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .fillMaxHeight()
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

enum class BottomBarDestination(val icon: ImageVector) {
    Home(Yume.House),
    Proxy(Yume.ArrowDownUp),
    Config(Yume.PackageCheck);

    val label: String
        get() =
            when (this) {
                Home -> YumeTxt.Component.BottomBar.Home
                Proxy -> YumeTxt.Component.BottomBar.Proxy
                Config -> YumeTxt.Component.BottomBar.Config
            }
}
