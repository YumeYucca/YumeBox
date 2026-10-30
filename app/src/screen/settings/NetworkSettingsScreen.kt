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
    val runMode = screen.runMode
    // Root modes are only selectable when root is granted; otherwise their list entries are
    // greyed out.
    val rootAvailable = screen.rootAvailable
    val ebpfAvailable = screen.ebpfAvailable
    var showKernelDialog by remember { mutableStateOf(false) }

    GlassScaffold(
        topBar = { TopBar(title = YumeTxt.NetworkSettings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item {
                RunModeSection(
                    viewModel = viewModel,
                    runMode = runMode,
                    rootAvailable = rootAvailable,
                    ebpfAvailable = ebpfAvailable,
                )
            }
            item {
                AdvancedOptionsSection(
                    viewModel = viewModel,
                    runMode = runMode,
                    disableAllOverride = disableAllOverride,
                    navigator = navigator,
                )
            }
            item {
                KernelSection(
                    screen = screen,
                    viewModel = viewModel,
                    onRequestRefresh = { showKernelDialog = true },
                )
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

/** The run-mode dropdown and the note explaining what each mode needs. */
@Composable
private fun RunModeSection(
    viewModel: NetworkSettingsViewModel,
    runMode: RunMode,
    rootAvailable: Boolean,
    ebpfAvailable: Boolean,
) {
    Title(YumeTxt.NetworkSettings.RunMode.SectionTitle)
    AppCard {
        // Same dropdown row as the active-kernel picker. Every mode stays listed; a
        // mode missing root or an eBPF-capable kernel is greyed out instead of hidden.
        GlassDropdownPreference(
            options =
                RunMode.entries.map { mode ->
                    GlassDropdownOption(
                        value = mode,
                        label = mode.title,
                        icon = mode.icon,
                        enabled =
                            when (mode) {
                                RunMode.VpnService -> true
                                RunMode.Tun -> rootAvailable
                                RunMode.Ebpf -> rootAvailable && ebpfAvailable
                            },
                    )
                },
            selected = runMode,
            title = YumeTxt.NetworkSettings.RunMode.CurrentTitle,
            onSelected = viewModel::onRunModeChange,
        )
    }
    // One short note supplementing the row above: what each mode needs.
    SectionSummary(YumeTxt.NetworkSettings.RunMode.Notice)
}

/** Advanced options: the per-mode service config page and the disable-overrides switch. */
@Composable
private fun AdvancedOptionsSection(
    viewModel: NetworkSettingsViewModel,
    runMode: RunMode,
    disableAllOverride: Boolean,
    navigator: Navigator,
) {
    Title(YumeTxt.NetworkSettings.Section.Advanced)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.Section.VpnOptions,
            enabled = runMode != RunMode.Ebpf,
            // Each mode's service config lives behind its own page.
            onClick = {
                when (runMode) {
                    RunMode.VpnService -> navigator.push(Route.VpnServiceOptions)
                    RunMode.Tun -> navigator.push(Route.TunServiceOptions)
                    RunMode.Ebpf -> Unit
                }
            },
        )
    }
    SectionSummary(YumeTxt.NetworkSettings.Advanced.ServiceOptionsSummary)

    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.NetworkSettings.Advanced.DisableOverrideTitle,
            checked = disableAllOverride,
            onCheckedChange = viewModel::onDisableAllOverrideChange,
        )
    }
    SectionSummary(YumeTxt.NetworkSettings.Advanced.DisableOverrideSummary)
}

/** Installed-kernel picker plus the refresh row that opens the download dialog. */
@Composable
private fun KernelSection(
    screen: NetworkSettingsViewModel.NetworkSettingsScreenState,
    viewModel: NetworkSettingsViewModel,
    onRequestRefresh: () -> Unit,
) {
    Title(YumeTxt.NetworkSettings.Section.Kernel)
    AppCard {
        // Installed kernels come from the view model, which reads them off the main
        // thread; probing them here would run an ELF scan per candidate per frame.
        val installed = screen.installedKernels.associateBy { it.id }
        val kernelIds =
            (listOf(KernelManager.BUNDLED_ALPHA_ID) + installed.keys).distinct()
        GlassDropdownPreference(
            title = YumeTxt.NetworkSettings.Kernel.ActiveTitle,
            summary = null,
            options =
                kernelIds.map { id ->
                    GlassDropdownOption(
                        id,
                        kernelLabel(id, installed[id]?.commit, installed[id]?.name),
                    )
                },
            selected = screen.activeKernelId,
            enabled = !screen.kernelBusy,
            onSelected = viewModel::selectKernel,
        )
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.Kernel.RefreshTitle,
            summary = null,
            onClick = onRequestRefresh,
        )
    }
    SectionSummary(YumeTxt.NetworkSettings.Kernel.SectionSummary)
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

private fun kernelLabel(id: String, commit: String? = null, name: String? = null): String {
    val label = when (id) {
        KernelManager.BUNDLED_ALPHA_ID -> YumeTxt.NetworkSettings.Kernel.BundledAlpha
        "alpha" -> "Alpha"
        "mate" -> "Mate"
        "meta" -> "Meta"
        "smart" -> "Smart"
        "ebpf" -> "eBPF"
        else -> name ?: id
    }
    return if (id == KernelManager.BUNDLED_ALPHA_ID || commit == null) {
        label
    } else {
        "$label-${commit.take(6)}"
    }
}

/** What the kernel download dialog shows: a spinner, a fetch button, or the kernel list. */
private enum class KernelDialogContent { Busy, Empty, List }

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
    val toggleSelection: (String) -> Unit = { id ->
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }
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
        val content = when {
            screen.kernelBusy -> KernelDialogContent.Busy
            screen.kernels.isEmpty() -> KernelDialogContent.Empty
            else -> KernelDialogContent.List
        }
        AnimatedContent(
            targetState = content,
            transitionSpec = {
                fadeIn(tween(140)) togetherWith fadeOut(tween(90)) using SizeTransform(clip = false)
            },
            label = "kernel_dialog_content",
        ) { state ->
            when (state) {
                KernelDialogContent.Busy ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space24),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        InfiniteProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                KernelDialogContent.Empty ->
                    TextButton(
                        text = YumeTxt.NetworkSettings.Kernel.FetchButton,
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.space8),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                KernelDialogContent.List ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                        screen.kernels.map { kernel ->
                            val installedName =
                                screen.installedKernels.firstOrNull { it.id == kernel.id }?.name
                            kernel.id to kernelLabel(kernel.id, kernel.commit, installedName)
                        }
                            .forEach { (id, label) ->
                                BasicComponent(
                                    title = label,
                                    onClick = { toggleSelection(id) },
                                    endActions = {
                                        Checkbox(
                                            state = ToggleableState(selectedIds.contains(id)),
                                            onClick = { toggleSelection(id) },
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
