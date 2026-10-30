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

package com.github.yumeyucca.yumebox.screen.settings

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.NotificationDisplaySettings
import com.github.yumeyucca.yumebox.data.model.NotificationLineContent
import com.github.yumeyucca.yumebox.data.model.NotificationLineValues
import com.github.yumeyucca.yumebox.data.model.resolve
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumeyucca.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumeyucca.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumeyucca.yumebox.presentation.component.StandalonePageList
import com.github.yumeyucca.yumebox.presentation.component.SectionSummary
import com.github.yumeyucca.yumebox.presentation.component.rememberAppIconBitmap
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/** Entries the first notification line can show; the second line may also stay [NotificationLineContent.Hidden]. */
private val firstLineOptions =
    NotificationLineContent.entries.filter { it != NotificationLineContent.Hidden }

/** The notification preview badge mirrors the real one, so it decodes at 2x its 40dp draw size. */
private const val NotificationPreviewIconPx = 96

private const val SAMPLE_NODE = "HK-01"
private const val SAMPLE_USED_BYTES = 13_314_398_618L
private const val SAMPLE_TOTAL_BYTES = 193_273_528_320L
private const val SAMPLE_SESSION_BYTES = 2_684_354_560L
private const val SAMPLE_DOWN_SPEED = 1_258_291L
private const val SAMPLE_UP_SPEED = 122_880L

@Composable
fun NotificationSettingsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.AppSettings.Notification.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->

        NotificationSettingsContent(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
            viewModel = viewModel,
            homeViewModel = homeViewModel,
        )
    }
}

/**
 * Collects the notification settings state and lays the three sections into the page list. The
 * preview state lives in [NotificationPreviewSection] because nothing else renders it.
 */
@Composable
private fun NotificationSettingsContent(
    scrollBehavior: ScrollBehavior,
    scaffoldPadding: PaddingValues,
    viewModel: AppSettingsViewModel,
    homeViewModel: HomeViewModel,
) {
    val showTraffic by viewModel.showTrafficNotification.state.collectAsState()
    val firstLine by viewModel.notificationFirstLine.state.collectAsState()
    val secondLine by viewModel.notificationSecondLine.state.collectAsState()
    val firstLineText by viewModel.notificationFirstLineText.state.collectAsState()
    val secondLineText by viewModel.notificationSecondLineText.state.collectAsState()
    val superIslandEnabled by viewModel.superIslandEnabled.state.collectAsState()
    val shizukuAccess by viewModel.shizukuAccess.collectAsState()

    // Refreshes on first composition and on every resume, matching what the section shows.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshShizukuAccess()
        onPauseOrDispose { }
    }

    val displaySettings =
        remember(firstLine, secondLine, firstLineText, secondLineText) {
            NotificationDisplaySettings(
                firstLine = firstLine,
                secondLine = secondLine,
                firstLineText = firstLineText,
                secondLineText = secondLineText,
            )
        }

    StandalonePageList(
        scrollBehavior = scrollBehavior,
        scaffoldPadding = scaffoldPadding,
    ) {
        item {
            NotificationPreviewSection(
                homeViewModel = homeViewModel,
                displaySettings = displaySettings,
            )
        }
        item {
            NotificationContentSection(
                viewModel = viewModel,
                showTraffic = showTraffic,
                displaySettings = displaySettings,
            )
        }
        if (shizukuAccess.islandSupported) {
            item {
                NotificationHyperOsSection(
                    viewModel = viewModel,
                    superIslandEnabled = superIslandEnabled,
                    shizukuAccess = shizukuAccess,
                )
            }
        }
    }
}

/** The preview card plus the live session state it draws. */
@Composable
private fun NotificationPreviewSection(
    homeViewModel: HomeViewModel,
    displaySettings: NotificationDisplaySettings,
) {
    val context = LocalContext.current
    val profile by homeViewModel.currentProfile.collectAsState()
    val currentNode by homeViewModel.selectedServerName.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val trafficTotal by homeViewModel.trafficTotal.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()

    val appName =
        remember(context) {
            context.applicationInfo.loadLabel(context.packageManager).toString()
        }
    val appIcon = rememberAppIconBitmap(context.packageName, NotificationPreviewIconPx)
    val traffic = remember(trafficNow, isRunning) {
        if (isRunning) TrafficData.from(trafficNow) else TrafficData.zero
    }
    val sessionTraffic = remember(trafficTotal, isRunning) {
        if (isRunning) TrafficData.from(trafficTotal) else TrafficData.zero
    }
    val previewValues =
        remember(profile, currentNode, traffic, sessionTraffic, isRunning, appName) {
            buildPreviewValues(
                profile = profile,
                currentNode = currentNode,
                traffic = traffic,
                sessionTraffic = sessionTraffic,
                isRunning = isRunning,
                appName = appName,
            )
        }

    Title(YumeTxt.AppSettings.Notification.PreviewSection)
    NotificationPreviewCard(
        appIcon = appIcon,
        values = previewValues,
        displaySettings = displaySettings,
    )
}

/** What the notification says: the traffic switch, both line pickers and their custom text. */
@Composable
private fun NotificationContentSection(
    viewModel: AppSettingsViewModel,
    showTraffic: Boolean,
    displaySettings: NotificationDisplaySettings,
) {
    Title(YumeTxt.AppSettings.Notification.ContentSection)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Notification.ShowTrafficTitle,
            checked = showTraffic,
            onCheckedChange = viewModel::onShowTrafficNotificationChange,
        )
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Notification.FirstLineTitle,
            currentValue = displaySettings.firstLine,
            values = firstLineOptions,
            onValueChange = viewModel::onNotificationFirstLineChange,
            label = { lineContentLabel(it) },
        )
        NotificationCustomTextRow(
            selected = displaySettings.firstLine,
            value = displaySettings.firstLineText,
            onValueChange = viewModel::onNotificationFirstLineTextChange,
        )
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Notification.SecondLineTitle,
            currentValue = displaySettings.secondLine,
            values = NotificationLineContent.entries,
            onValueChange = viewModel::onNotificationSecondLineChange,
            label = { lineContentLabel(it) },
        )
        NotificationCustomTextRow(
            selected = displaySettings.secondLine,
            value = displaySettings.secondLineText,
            onValueChange = viewModel::onNotificationSecondLineTextChange,
        )
    }
}

/** HyperOS-only section: Super Island switch and the Shizuku access row. */
@Composable
private fun NotificationHyperOsSection(
    viewModel: AppSettingsViewModel,
    superIslandEnabled: Boolean,
    shizukuAccess: AppSettingsViewModel.ShizukuAccessState,
) {
    Title(YumeTxt.AppSettings.Notification.HyperOsSection)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.ServiceSection.SuperIslandTitle,
            checked = superIslandEnabled,
            onCheckedChange = viewModel::onSuperIslandEnabledChange,
        )
        PreferenceArrowItem(
            title = YumeTxt.AppSettings.ServiceSection.ShizukuTitle,
            endActions = {
                Text(
                    text = shizukuAccess.statusText,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            },
            onClick = viewModel::onShizukuAccessClick,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Notification.HyperOsSummary)
}

/**
 * Editable custom text of one notification line; the row only exists while that line is
 * [NotificationLineContent.CustomText], so the choice and its editor stay in one place.
 */
@Composable
private fun NotificationCustomTextRow(
    selected: NotificationLineContent,
    value: String,
    onValueChange: (String) -> Unit,
) {
    if (selected != NotificationLineContent.CustomText) return
    NotificationCustomTextPreference(
        value = value,
        onValueChange = onValueChange,
    )
}

@Composable
private fun NotificationCustomTextPreference(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value) }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Notification.CustomTextTitle,
        onClick = {
            draft = value
            showDialog = true
        },
        holdDownState = showDialog,
    )
    AppTextFieldDialog(
        show = showDialog,
        title = YumeTxt.AppSettings.Notification.CustomTextTitle,
        value = draft,
        onValueChange = { draft = it },
        onDismissRequest = { showDialog = false },
        onConfirm = {
            onValueChange(draft)
            showDialog = false
        },
        singleLine = true,
    )
}

/**
 * Builds the notification-line values shown in the preview, falling back to representative sample
 * data whenever a live value is missing. Every fallback is non-empty, so the formatted fields below
 * never render blank.
 */
private fun buildPreviewValues(
    profile: Profile?,
    currentNode: String?,
    traffic: TrafficData,
    sessionTraffic: TrafficData,
    isRunning: Boolean,
    appName: String,
): NotificationLineValues {
    val usedBytes =
        profile
            ?.let { (it.upload + it.download).coerceAtLeast(0L) }
            ?.takeIf { it > 0L }
            ?: SAMPLE_USED_BYTES
    val totalBytes =
        profile?.total?.coerceAtLeast(0L)?.takeIf { it > 0L } ?: SAMPLE_TOTAL_BYTES
    val sessionBytes =
        (sessionTraffic.download + sessionTraffic.upload)
            .coerceAtLeast(0L)
            .takeIf { isRunning && it > 0L }
            ?: SAMPLE_SESSION_BYTES
    val expiry =
        profile?.expire?.takeIf { it > 0L }?.let(::formatExpiryDate)
            ?: LocalDate.now().plusDays(7).toString()
    val downSpeed = if (isRunning) traffic.download else SAMPLE_DOWN_SPEED
    val upSpeed = if (isRunning) traffic.upload else SAMPLE_UP_SPEED
    return NotificationLineValues(
        profileName = profile?.name?.takeIf { it.isNotBlank() } ?: appName,
        currentNode =
            if (isRunning) {
                currentNode?.takeIf { it.isNotBlank() }
                    ?: YumeTxt.Service.Notification.NoNode
            } else {
                SAMPLE_NODE
            },
        liveTraffic =
            YumeTxt.Service.Notification.SpeedLine.format(
                ByteFormatter.formatSpeed(downSpeed),
                ByteFormatter.formatSpeed(upSpeed),
            ),
        usedTraffic = ByteFormatter.format(usedBytes),
        sessionTraffic =
            YumeTxt.Service.Notification.TotalTraffic.format(
                ByteFormatter.format(sessionBytes),
            ),
        totalTraffic = ByteFormatter.format(totalBytes),
        remainingTraffic = ByteFormatter.format((totalBytes - usedBytes).coerceAtLeast(0L)),
        subscriptionUsage = "${ByteFormatter.format(usedBytes)} / ${ByteFormatter.format(totalBytes)} | $expiry",
        expiryDate = expiry,
        status = YumeTxt.Service.Notification.Running,
    )
}

@Composable
private fun NotificationPreviewCard(
    appIcon: ImageBitmap?,
    values: NotificationLineValues,
    displaySettings: NotificationDisplaySettings,
) {
    val title =
        displaySettings.firstLine
            .resolve(values, displaySettings.firstLineText)
            .ifBlank { values.profileName }
    val content =
        displaySettings.secondLine.resolve(values, displaySettings.secondLineText)
    val context = LocalContext.current
    val previewTime =
        remember(context) {
            DateFormat.getTimeFormat(context).format(Date())
        }

    AppCard(insideMargin = PaddingValues(horizontal = UiDp.dp14, vertical = UiDp.dp10)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(UiDp.dp40)
                            .clip(RoundedCornerShape(UiDp.dp10)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(UiDp.dp8))
                    Text(
                        text = previewTime,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        maxLines = 1,
                    )
                }
                if (content.isNotBlank()) {
                    Text(
                        text = content,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Locale-aware label for [content], usable directly as the selector's label lambda. */
private fun lineContentLabel(content: NotificationLineContent): String =
    when (content) {
        NotificationLineContent.ProfileName ->
            YumeTxt.AppSettings.Notification.LineProfileName
        NotificationLineContent.Status ->
            YumeTxt.AppSettings.Notification.LineStatus
        NotificationLineContent.CurrentNode ->
            YumeTxt.AppSettings.Notification.LineCurrentNode
        NotificationLineContent.LiveTraffic ->
            YumeTxt.AppSettings.Notification.LineLiveTraffic
        NotificationLineContent.UsedTraffic ->
            YumeTxt.AppSettings.Notification.LineUsedTraffic
        NotificationLineContent.SessionTraffic ->
            YumeTxt.AppSettings.Notification.LineSessionTraffic
        NotificationLineContent.TotalTraffic ->
            YumeTxt.AppSettings.Notification.LineTotalTraffic
        NotificationLineContent.RemainingTraffic ->
            YumeTxt.AppSettings.Notification.LineRemainingTraffic
        NotificationLineContent.SubscriptionUsage ->
            YumeTxt.AppSettings.Notification.LineSubscriptionUsage
        NotificationLineContent.ExpiryDate ->
            YumeTxt.AppSettings.Notification.LineExpiryDate
        NotificationLineContent.CustomText ->
            YumeTxt.AppSettings.Notification.LineCustomText
        NotificationLineContent.Hidden ->
            YumeTxt.AppSettings.Notification.LineHidden
    }

private fun formatExpiryDate(expireAt: Long): String =
    Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()
