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

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun HomePreviewGuideDialog(
    show: Boolean,
    useSystemWallpaper: Boolean,
    onUseSystemWallpaperChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var page by remember { mutableStateOf(GuidePage.Preview) }
    var accessGranted by remember { mutableStateOf(SystemWallpaperAccess.isGranted(context)) }
    LaunchedEffect(show) {
        if (show) page = GuidePage.Preview
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = SystemWallpaperAccess.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val configuration = LocalConfiguration.current
    // Keep the dialog body within ~70% of screen height so title/buttons stay reachable.
    val contentMaxHeight = (configuration.screenHeightDp * 0.70f).dp

    WindowDialog(
        show = show,
        modifier = Modifier,
        title = page.text.title,
        titleColor = DialogDefaults.titleColor(),
        summary = page.text.summary,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = {},
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = contentMaxHeight)
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space18),
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(320)) { w -> if (forward) w else -w } +
                                fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(tween(320)) { w -> if (forward) -w else w } +
                                        fadeOut(tween(180))) using
                                SizeTransform(clip = false)
                    },
                    label = "guide_page",
                ) { p ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (p) {
                            GuidePage.Preview -> MoeHomeSkeletonMockup(demo = HomeMockupDemo.StartButton)
                            GuidePage.Wallpaper -> MoeHomeSkeletonMockup(demo = HomeMockupDemo.Wallpaper)
                            GuidePage.Swipe -> HomeToNodeSwipeMockup()
                            GuidePage.Add ->
                                MoeHomeSkeletonMockup(
                                    demo = HomeMockupDemo.StartButton,
                                    animateLaunch = false,
                                    markWallpaper = true,
                                )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GuidePage.entries.forEach { entry ->
                        Box(
                            modifier =
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (entry == page) colorScheme.primary
                                        else colorScheme.onSurface.copy(alpha = opacity.muted)
                                    )
                        )
                    }
                }

                if (page != GuidePage.Add) {
                    TextButton(
                        text = YumeTxt.Home.PreviewGuide.Next,
                        onClick = { page = GuidePage.entries[page.ordinal + 1] },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space12)) {
                        SystemWallpaperPreferenceItem(
                            checked = useSystemWallpaper,
                            onCheckedChange = onUseSystemWallpaperChange,
                        )
                        TextButton(
                            text = YumeTxt.Home.PreviewGuide.Start,
                            onClick = { if (accessGranted) onDismissRequest() },
                            enabled = accessGranted,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    )
}

/** The four panels of the guided tour; the order is the paging order. */
private enum class GuidePage {
    Preview,
    Wallpaper,
    Swipe,
    Add,
    ;

    val text: GuidePageText
        get() = when (this) {
            Preview ->
                GuidePageText(
                    YumeTxt.Home.PreviewGuide.Title,
                    YumeTxt.Home.PreviewGuide.Description,
                )
            Wallpaper ->
                GuidePageText(
                    YumeTxt.Home.PreviewGuide.WallpaperTitle,
                    YumeTxt.Home.PreviewGuide.WallpaperDescription,
                )
            Swipe ->
                GuidePageText(
                    YumeTxt.Home.PreviewGuide.SwipeTitle,
                    YumeTxt.Home.PreviewGuide.SwipeDescription,
                )
            Add ->
                GuidePageText(
                    YumeTxt.Home.PreviewGuide.AddTitle,
                    YumeTxt.Home.PreviewGuide.AddDescription,
                )
        }
}

private data class GuidePageText(val title: String, val summary: String)
