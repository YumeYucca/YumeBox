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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.common.util.SubscriptionUserAgentDefaults
import com.github.yumeyucca.yumebox.common.util.startActivitySafely
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AppLanguage
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.screen.settings.component.ThemeColorPickerItem
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.yumeyucca.yumebox.presentation.theme.label

@Composable
fun AppSettingsScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<AppSettingsViewModel>()

    GlassScaffold(
        topBar = { TopBar(title = YumeTxt.AppSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item { AppBehaviorSettingsSection(viewModel) }
            item { AppThemeModeSettingsSection(viewModel) }
            item { AppThemeColorSettingsSection(viewModel) }
            item {
                AppNotificationSettingsSection(
                    onOpenNotificationSettings = { navigator.push(Route.NotificationSettings) },
                )
            }
            item { AppInterfaceSettingsSection(viewModel) }
            item { AppNavigationSettingsSection(viewModel) }
            item { AppPrivacySettingsSection(viewModel) }
            item { AppServiceSettingsSection(viewModel) }
            item { AppNetworkSettingsSection(viewModel) }
        }
    }
}

@Composable
private fun AppBehaviorSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.behaviorSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Behavior)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Behavior.AutoStartTitle,
            checked = section.automaticRestart,
            onCheckedChange = viewModel::onAutomaticRestartChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Behavior.AutoUpdateOnStartTitle,
            checked = section.autoUpdateCurrentProfileOnStart,
            onCheckedChange = viewModel::onAutoUpdateCurrentProfileOnStartChange,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.BehaviorSummary)
}

@Composable
private fun AppThemeModeSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.interfaceSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Interface.ThemeModeTitle)
    AppCard {
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Interface.ThemeModeTitle,
            currentValue = section.themeMode,
            values = ThemeMode.entries,
            onValueChange = viewModel::onThemeModeChange,
            label = { mode -> mode.label },
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.ThemeSummary)
}

@Composable
private fun AppThemeColorSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.interfaceSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Interface.ColorThemeTitle)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.ThemeColorPolarityInvertTitle,
            checked = section.invertOnPrimaryColors,
            onCheckedChange = viewModel::onInvertOnPrimaryColorsChange,
        )
        ThemeColorPickerItem(
            themeSeedColorArgb = section.themeSeedColorArgb,
            onThemeSeedColorChange = viewModel::onThemeSeedColorChange,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.ColorSummary)
}

@Composable
private fun AppNotificationSettingsSection(onOpenNotificationSettings: () -> Unit) {
    Title(YumeTxt.AppSettings.Notification.Title)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.AppSettings.Notification.ContentSection,
            onClick = onOpenNotificationSettings,
        )
    }
}

@Composable
private fun AppInterfaceSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.interfaceSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Interface)
    AppCard {
        PreferenceEnumItem(
            title = YumeTxt.AppSettings.Interface.LanguageTitle,
            currentValue = section.appLanguage,
            values = AppLanguage.entries,
            onValueChange = viewModel::onAppLanguageChange,
            label = { language ->
                when (language) {
                    AppLanguage.System -> YumeTxt.AppSettings.Interface.LanguageSystem
                    AppLanguage.Zh -> YumeTxt.AppSettings.Interface.LanguageChinese
                    AppLanguage.ZhHant -> YumeTxt.AppSettings.Interface.LanguageChineseTraditional
                    AppLanguage.En -> YumeTxt.AppSettings.Interface.LanguageEnglish
                    AppLanguage.Ja -> YumeTxt.AppSettings.Interface.LanguageJapanese
                    AppLanguage.Ru -> YumeTxt.AppSettings.Interface.LanguageRussian
                }
            },
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.AutoHideNavbarTitle,
            checked = section.bottomBarAutoHide,
            onCheckedChange = viewModel::onBottomBarAutoHideChange,
        )
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.TopBarBlurTitle,
            checked = section.topBarBlurEnabled,
            onCheckedChange = viewModel::onTopBarBlurEnabledChange,
        )
        PageScalePreferenceItem(
            pageScale = section.pageScale,
            onApply = viewModel::onPageScaleChange,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.InterfaceSummary)
}

@Composable
private fun AppNavigationSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current
    val predictiveBackEnabled by viewModel.predictiveBackEnabled.state.collectAsState()
    val predictiveBackMaxProgress by viewModel.predictiveBackMaxProgress.state.collectAsState()

    Title(YumeTxt.AppSettings.Section.Navigation)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Interface.PredictiveBackTitle,
            checked = predictiveBackEnabled,
            onCheckedChange = { enabled ->
                viewModel.onPredictiveBackEnabledChange(enabled)
                context.toast(YumeTxt.AppSettings.Interface.PredictiveBackRestartSummary)
            },
        )
        PredictiveBackProgressPreferenceItem(
            progress = predictiveBackMaxProgress,
            onApply = viewModel::onPredictiveBackMaxProgressChange,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.NavigationSummary)
}

@Composable
private fun AppPrivacySettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.privacySectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Privacy)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Privacy.HideFromRecentsTitle,
            checked = section.excludeFromRecents,
            onCheckedChange = viewModel::onExcludeFromRecentsChange,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.PrivacySummary)
}

@Composable
private fun AppServiceSettingsSection(viewModel: AppSettingsViewModel) {
    val context = LocalContext.current

    Title(YumeTxt.AppSettings.Section.Service)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.AppSettings.ServiceSection.BatteryOptimizationTitle,
            onClick = {
                if (isBatteryOptimizationIgnored(context)) {
                    context.toast(YumeTxt.AppSettings.ServiceSection.BatteryOptimizationAlreadyDisabled)
                } else {
                    context.startActivitySafely(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    )
                }
            },
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.ServiceSummary)
}

@Composable
private fun AppNetworkSettingsSection(viewModel: AppSettingsViewModel) {
    val section by viewModel.networkSectionState.collectAsState()

    Title(YumeTxt.AppSettings.Section.Network)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.AppSettings.Network.AutoCheckUpdateTitle,
            checked = section.autoCheckUpdate,
            onCheckedChange = viewModel::onAutoCheckUpdateChange,
        )
        CustomUserAgentPreferenceItem(
            customUserAgent = section.customUserAgent,
            onConfirm = viewModel::applyCustomUserAgent,
        )
    }
    SectionSummary(YumeTxt.AppSettings.Appearance.NetworkSummary)
}

@SuppressLint("BatteryLife")
private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@Composable
private fun PageScalePreferenceItem(pageScale: Float, onApply: (Float) -> Unit) {
    var pageScaleLocal by remember(pageScale) { mutableFloatStateOf(pageScale) }
    var showPageScaleDialog by remember { mutableStateOf(false) }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Interface.PageScaleTitle,
        endActions = {
            Text(
                text = "${(pageScaleLocal * 100).toInt()}%",
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = { showPageScaleDialog = true },
        holdDownState = showPageScaleDialog,
        bottomAction = {
            Slider(
                value = pageScaleLocal,
                onValueChange = { pageScaleLocal = it },
                onValueChangeFinished = { onApply(pageScaleLocal) },
                valueRange = 0.8f..1.2f,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )

    PageScaleDialog(
        show = showPageScaleDialog,
        pageScale = pageScaleLocal,
        onApply = { scale ->
            pageScaleLocal = scale
            onApply(scale)
        },
        onDismissRequest = { showPageScaleDialog = false },
    )
}

@Composable
private fun PageScaleDialog(
    show: Boolean,
    pageScale: Float,
    onApply: (Float) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var scaleText by
        remember(show, pageScale) { mutableStateOf((pageScale * 100).toInt().toString()) }

    AppTextFieldDialog(
        show = show,
        title = YumeTxt.AppSettings.Interface.PageScaleTitle,
        value = scaleText,
        onValueChange = { value ->
            if (value.isEmpty() || value.all(Char::isDigit)) {
                scaleText = value
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = {
            val percent = (scaleText.toFloatOrNull() ?: (pageScale * 100)).coerceIn(80f, 120f)
            onApply(percent / 100f)
            onDismissRequest()
        },
        summary = YumeTxt.AppSettings.Interface.PageScaleDialogSummary,
        singleLine = true,
        trailingIcon = {
            Text(
                text = "%",
                modifier = Modifier.padding(horizontal = UiDp.dp16),
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
    )
}

@Composable
private fun PredictiveBackProgressPreferenceItem(
    progress: Float,
    onApply: (Float) -> Unit,
) {
    var localProgress by remember(progress) { mutableFloatStateOf(progress) }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Interface.PredictiveBackProgressTitle,
        endActions = {
            Text(
                text = "${localProgress.toInt()}%",
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = {},
        bottomAction = {
            Slider(
                value = localProgress,
                onValueChange = { localProgress = it },
                onValueChangeFinished = { onApply(localProgress) },
                valueRange = 1f..100f,
                magnetThreshold = 0.01f,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            )
        },
    )
}

@Composable
private fun CustomUserAgentPreferenceItem(customUserAgent: String, onConfirm: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    var showEditDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(customUserAgent) }
    val effectiveUserAgent = customUserAgent.ifBlank { SubscriptionUserAgentDefaults.DEFAULT }

    fun applyDraft() {
        onConfirm(draft)
        focusManager.clearFocus()
        showEditDialog = false
    }

    PreferenceArrowItem(
        title = YumeTxt.AppSettings.Network.UserAgentTitle,
        endActions = {
            Text(
                text = effectiveUserAgent,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        },
        onClick = {
            draft = customUserAgent
            showEditDialog = true
        },
        holdDownState = showEditDialog,
    )

    AppTextFieldDialog(
        show = showEditDialog,
        title = YumeTxt.AppSettings.EditDialog.UserAgentTitle,
        value = draft,
        onValueChange = { draft = it },
        onDismissRequest = {
            showEditDialog = false
            focusManager.clearFocus()
        },
        onConfirm = ::applyDraft,
        singleLine = true,
        onImeAction = ::applyDraft,
    )
}
