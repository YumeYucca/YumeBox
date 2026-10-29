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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.common.util.formatSpeed
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
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.component.SectionSummary
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val firstLineOptions =
    NotificationLineContent.entries.filter { it != NotificationLineContent.Hidden }
private val otherLineOptions = NotificationLineContent.entries
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val showTraffic by viewModel.showTrafficNotification.state.collectAsState()
    val firstLine by viewModel.notificationFirstLine.state.collectAsState()
    val secondLine by viewModel.notificationSecondLine.state.collectAsState()
    val firstLineText by viewModel.notificationFirstLineText.state.collectAsState()
    val secondLineText by viewModel.notificationSecondLineText.state.collectAsState()
    val superIslandEnabled by viewModel.superIslandEnabled.state.collectAsState()
    val shizukuAccess by viewModel.shizukuAccess.collectAsState()

    val profile by homeViewModel.currentProfile.collectAsState()
    val currentNode by homeViewModel.selectedServerName.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val trafficTotal by homeViewModel.trafficTotal.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshShizukuAccess()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val appName = remember(context) {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }
    val appIcon = remember(context) {
        context.packageManager
            .getApplicationIcon(context.applicationInfo)
            .toBitmap(96, 96)
            .asImageBitmap()
    }
    val traffic = remember(trafficNow, isRunning) {
        if (isRunning) TrafficData.from(trafficNow) else TrafficData.zero
    }
    val sessionTraffic = remember(trafficTotal, isRunning) {
        if (isRunning) TrafficData.from(trafficTotal) else TrafficData.zero
    }
    val previewValues =
        remember(profile, currentNode, traffic, sessionTraffic, isRunning, appName) {
            val sampleExpiry = LocalDate.now().plusDays(7).toString()
            val used =
                profile
                    ?.let { (it.upload + it.download).coerceAtLeast(0L) }
                    ?.takeIf { it > 0L }
                    ?: SAMPLE_USED_BYTES
            val total =
                profile?.total?.coerceAtLeast(0L)?.takeIf { it > 0L } ?: SAMPLE_TOTAL_BYTES
            val expire = profile?.expire?.takeIf { it > 0L }
            val profileName = profile?.name?.takeIf { it.isNotBlank() } ?: appName
            val sessionBytes =
                (sessionTraffic.download + sessionTraffic.upload)
                    .coerceAtLeast(0L)
                    .takeIf { isRunning && it > 0L }
                    ?: SAMPLE_SESSION_BYTES
            val usage =
                when {
                    total > 0L -> "${formatBytes(used)} / ${formatBytes(total)}"
                    used > 0L -> formatBytes(used)
                    else -> ""
                }
            val expiry = expire?.let(::formatExpiryDate) ?: sampleExpiry
            val node =
                if (isRunning) {
                    currentNode?.takeIf { it.isNotBlank() }
                        ?: YumeTxt.Service.Notification.NoNode
                } else {
                    SAMPLE_NODE
                }
            val downSpeed =
                if (isRunning) traffic.download else SAMPLE_DOWN_SPEED
            val upSpeed =
                if (isRunning) traffic.upload else SAMPLE_UP_SPEED
            NotificationLineValues(
                profileName = profileName,
                currentNode = node,
                liveTraffic =
                    YumeTxt.Service.Notification.SpeedLine.format(
                        formatSpeed(downSpeed),
                        formatSpeed(upSpeed),
                    ),
                usedTraffic = formatBytes(used),
                sessionTraffic =
                    YumeTxt.Service.Notification.TotalTraffic.format(
                        formatBytes(sessionBytes),
                    ),
                totalTraffic = if (total > 0L) formatBytes(total) else "—",
                remainingTraffic =
                    if (total > 0L) formatBytes((total - used).coerceAtLeast(0L)) else "—",
                subscriptionUsage =
                    listOfNotNull(usage.takeIf { it.isNotEmpty() }, expiry)
                        .joinToString(" | ")
                        .ifBlank { "—" },
                expiryDate = expiry,
                status = YumeTxt.Service.Notification.Running,
            )
        }
    val displaySettings =
        NotificationDisplaySettings(
            firstLine = firstLine,
            secondLine = secondLine,
            firstLineText = firstLineText,
            secondLineText = secondLineText,
        )

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.AppSettings.Notification.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.AppSettings.Notification.PreviewSection)
                NotificationPreviewCard(
                    appIcon = appIcon,
                    values = previewValues,
                    displaySettings = displaySettings,
                )
            }
            item {
                Title(YumeTxt.AppSettings.Notification.ContentSection)
                AppCard {
                    PreferenceSwitchItem(
                        title = YumeTxt.AppSettings.Notification.ShowTrafficTitle,
                        checked = showTraffic,
                        onCheckedChange = viewModel::onShowTrafficNotificationChange,
                    )
                    NotificationLinePreference(
                        title = YumeTxt.AppSettings.Notification.FirstLineTitle,
                        value = firstLine,
                        options = firstLineOptions,
                        onValueChange = viewModel::onNotificationFirstLineChange,
                    )
                    if (firstLine == NotificationLineContent.CustomText) {
                        NotificationCustomTextPreference(
                            title = YumeTxt.AppSettings.Notification.CustomTextTitle,
                            value = firstLineText,
                            onValueChange = viewModel::onNotificationFirstLineTextChange,
                        )
                    }
                    NotificationLinePreference(
                        title = YumeTxt.AppSettings.Notification.SecondLineTitle,
                        value = secondLine,
                        options = otherLineOptions,
                        onValueChange = viewModel::onNotificationSecondLineChange,
                    )
                    if (secondLine == NotificationLineContent.CustomText) {
                        NotificationCustomTextPreference(
                            title = YumeTxt.AppSettings.Notification.CustomTextTitle,
                            value = secondLineText,
                            onValueChange = viewModel::onNotificationSecondLineTextChange,
                        )
                    }
                }
            }
            if (shizukuAccess.islandSupported) {
                item {
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
            }
        }
    }
}

@Composable
private fun NotificationLinePreference(
    title: String,
    value: NotificationLineContent,
    options: List<NotificationLineContent>,
    onValueChange: (NotificationLineContent) -> Unit,
) {
    PreferenceEnumItem(
        title = title,
        currentValue = value,
        items = options.map { lineContentLabel(it) },
        values = options,
        onValueChange = onValueChange,
    )
}

@Composable
private fun NotificationCustomTextPreference(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value) }

    PreferenceArrowItem(
        title = title,
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

@Composable
private fun NotificationPreviewCard(
    appIcon: androidx.compose.ui.graphics.ImageBitmap,
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
            android.text.format.DateFormat.getTimeFormat(context).format(Date())
        }

    AppCard(insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
            )
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
                    Spacer(Modifier.width(8.dp))
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

@Composable
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
