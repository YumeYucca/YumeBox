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


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.PlaneTakeoff
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.runtime.service.core.KernelManager
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Ghost
import io.github.yumeyucca.lucide.lucide.MonitorSmartphone
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference as WindowDropdownPreference

/**
 * Network settings entry point. Top: a "run mode" dropdown row shaped like the active-kernel
 * picker, listing VpnService, Tun and eBPF. `VpnService` is always selectable; Tun needs root and
 * eBPF needs root plus an eBPF-capable kernel, and an unavailable mode is greyed out in the list
 * instead of being hidden. Below: advanced options (service config + disable-overrides) and the
 * kernel section.
 */
@Composable
fun NetworkSettingsScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val screen by viewModel.networkScreenState.collectAsState()
    val disableAllOverride = screen.disableAllOverride
    val accessControlMode = screen.accessControlMode
    val runMode = screen.runMode
    // Root modes are only selectable when root is granted; otherwise their list entries are
    // greyed out.
    val rootAvailable = screen.rootAvailable
    val ebpfAvailable = screen.ebpfAvailable
    val kernels = screen.kernels
    val context = LocalContext.current
    var showKernelDialog by remember { mutableStateOf(false) }

    GlassScaffold(
        topBar = { TopBar(title = YumeTxt.NetworkSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.NetworkSettings.RunMode.SectionTitle)
                AppCard {
                    // Same dropdown row as the active-kernel picker. Every mode stays listed; a
                    // mode missing root or an eBPF-capable kernel is greyed out instead of hidden.
                    WindowDropdownPreference(
                        items = RunMode.entries.map { it.title },
                        icons = RunMode.entries.map { it.icon },
                        itemEnabled =
                            RunMode.entries.map { mode ->
                                when (mode) {
                                    RunMode.VpnService -> true
                                    RunMode.Tun -> rootAvailable
                                    RunMode.Ebpf -> rootAvailable && ebpfAvailable
                                }
                            },
                        selectedIndex = RunMode.entries.indexOf(runMode),
                        title = YumeTxt.NetworkSettings.RunMode.CurrentTitle,
                        onSelectedIndexChange = { viewModel.onRunModeChange(RunMode.entries[it]) },
                    )
                }
                // One short note supplementing the row above: what each mode needs.
                SectionSummary(YumeTxt.NetworkSettings.RunMode.Notice)
            }
            item {
                Title(YumeTxt.NetworkSettings.Section.Advanced)
                AppCard {
                    PreferenceArrowItem(
                        title = YumeTxt.NetworkSettings.Section.VpnOptions,
                        // Each mode's service config lives behind its own page.
                        onClick = {
                            when (runMode) {
                                RunMode.VpnService -> navigator.push(Route.VpnServiceOptions)
                                RunMode.Tun -> navigator.push(Route.TunServiceOptions)
                                RunMode.Ebpf -> navigator.push(Route.EbpfServiceOptions)
                            }
                        },
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.Advanced.DisableOverrideTitle,
                        checked = disableAllOverride,
                        onCheckedChange = viewModel::onDisableAllOverrideChange,
                    )
                }
                SectionSummary(YumeTxt.NetworkSettings.Advanced.DisableOverrideSummary)
            }
            item {
                Title(YumeTxt.NetworkSettings.Section.Kernel)
                AppCard {
                    val installedKernelIds = kernels
                        .filter { KernelManager.isInstalled(context, it.id) }
                        .map { it.id }
                    val locallyInstalledKernelIds = KernelManager.installedKernelIds(context)
                    val kernelIds = listOf("bundled-alpha") +
                        (locallyInstalledKernelIds + installedKernelIds).distinct()
                    WindowDropdownPreference(
                        title = YumeTxt.NetworkSettings.Kernel.ActiveTitle,
                        summary = null,
                        items = kernelIds.map { id ->
                            kernelLabel(id, screen.installedKernelCommits[id])
                        },
                        selectedIndex = kernelIds.indexOf(screen.activeKernelId).coerceAtLeast(0),
                        enabled = !screen.kernelBusy,
                        onSelectedIndexChange = { viewModel.selectKernel(kernelIds[it]) },
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.NetworkSettings.Kernel.RefreshTitle,
                        summary = null,
                        onClick = { showKernelDialog = true },
                    )
                }
            }
        }

        KernelSelectionDialog(
            show = showKernelDialog,
            screen = screen,
            onRefresh = viewModel::refreshKernels,
            onDownload = { ids ->
                viewModel.downloadKernels(ids) { success ->
                    if (success) showKernelDialog = false
                }
            },
            onDismiss = { showKernelDialog = false },
        )
    }
}

/** Label of a run mode inside the mode dropdown. */
private val RunMode.title: String
    get() =
        when (this) {
            RunMode.VpnService -> YumeTxt.NetworkSettings.RunMode.VpnServiceTitle
            RunMode.Tun -> YumeTxt.NetworkSettings.RunMode.TunTitle
            RunMode.Ebpf -> YumeTxt.NetworkSettings.RunMode.EbpfTitle
        }

/** Leading icon of a run mode inside the mode dropdown. */
private val RunMode.icon: ImageVector
    get() =
        when (this) {
            RunMode.VpnService -> Yume.PlaneTakeoff
            RunMode.Tun -> Lucide.MonitorSmartphone
            RunMode.Ebpf -> Lucide.Ghost
        }

@Composable
private fun kernelLabel(id: String, commit: String? = null): String {
    val name = when (id) {
        "bundled-alpha" -> YumeTxt.NetworkSettings.Kernel.BundledAlpha
        "alpha" -> "Alpha"
        "mate" -> "Mate"
        "meta" -> "Meta"
        "smart" -> "Smart"
        "ebpf" -> "eBPF"
        else -> KernelManager.installedName(LocalContext.current, id)
    }
    return if (id == KernelManager.BUNDLED_ALPHA_ID || commit == null) {
        name
    } else {
        "$name-${commit.take(6)}"
    }
}

@Composable
private fun KernelSelectionDialog(
    show: Boolean,
    screen: NetworkSettingsViewModel.NetworkSettingsScreenState,
    onRefresh: () -> Unit,
    onDownload: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = AppTheme.spacing
    var selectedIds by remember(show) { mutableStateOf<Set<String>>(emptySet()) }
    var initialized by remember(show) { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (show) onRefresh()
    }
    LaunchedEffect(show, screen.kernels) {
        if (show && screen.kernels.isNotEmpty() && !initialized) {
            selectedIds = screen.kernels.map { it.id }.toSet()
            initialized = true
        }
    }
    AppDialog(show = show, title = YumeTxt.NetworkSettings.Kernel.DownloadTitle, onDismissRequest = onDismiss) {
        val contentState = when {
            screen.kernelBusy -> 0
            screen.kernels.isEmpty() -> 1
            else -> 2
        }
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false)
            },
            label = "kernel_dialog_content",
        ) { state ->
            when (state) {
                0 ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space24),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        InfiniteProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                1 ->
                    TextButton(
                        text = YumeTxt.NetworkSettings.Kernel.FetchButton,
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space8),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                else ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                        screen.kernels.map { it.id to kernelLabel(it.id, it.commit) }
                            .forEach { (id, label) ->
                                BasicComponent(
                                    title = label,
                                    onClick = {
                                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                    },
                                    endActions = {
                                        Checkbox(
                                            state = ToggleableState(selectedIds.contains(id)),
                                            onClick = {
                                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                            },
                                        )
                                    },
                                )
                            }
                        TextButton(
                            text = YumeTxt.NetworkSettings.Kernel.DownloadButton,
                            onClick = { onDownload(selectedIds) },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = spacing.space16),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
            }
        }
    }
}
