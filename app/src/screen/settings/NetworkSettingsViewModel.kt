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

package com.github.yumeyucca.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.github.yumeyucca.yumebox.data.controller.NetworkSettingsController
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.runtime.service.core.KernelManager
import com.github.yumeyucca.yumebox.runtime.service.root.RootAccessSupport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Backs both the run-mode picker ([NetworkSettingsScreen]) and the VpnService options page
 * ([VpnServiceOptionsScreen]). The parallel HTTP run mode is gone, so this exposes the
 * configured mode plus the VpnService knobs.
 */
class NetworkSettingsViewModel(
    application: Application,
    settings: NetworkSettingsStore,
    private val controller: NetworkSettingsController,
) : AndroidViewModel(application) {
    // Screens read these through the state snapshots below, so only the two rows that render their
    // own `Preference` stay public.
    private val runMode: Preference<RunMode> = settings.runMode
    private val bypassPrivateNetwork: Preference<Boolean> = settings.bypassPrivateNetwork
    private val dnsHijack: Preference<Boolean> = settings.dnsHijack
    private val allowBypass: Preference<Boolean> = settings.allowBypass
    private val enableIPv6: Preference<Boolean> = settings.enableIPv6
    private val systemProxy: Preference<Boolean> = settings.systemProxy
    private val disableAllOverride: Preference<Boolean> = settings.disableAllOverride
    private val accessControlMode: Preference<AccessControlMode> = settings.accessControlMode

    // Root Tun geometry — its own service-config sub-page ([TunServiceOptionsScreen]).
    val tunStack: Preference<TunStack> = settings.tunStack
    private val tunAutoRoute: Preference<Boolean> = settings.tunAutoRoute
    private val tunStrictRoute: Preference<Boolean> = settings.tunStrictRoute
    private val tunAutoRedirect: Preference<Boolean> = settings.tunAutoRedirect
    private val tunDnsMode: Preference<TunDnsMode> = settings.tunDnsMode
    private val tunIfName: Preference<String> = settings.tunIfName
    private val tunMtu: Preference<Int> = settings.tunMtu
    // Root availability, probed once on open — gates the Tun card (greyed when false, no toast).
    // Probing constructs the libsu shell, so a rooted device may surface its su prompt here; that
    // grant flow is intended. A non-rooted device fast-fails to false and the root cards stay
    // disabled.
    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable.asStateFlow()
    private val _ebpfAvailable = MutableStateFlow(false)
    val ebpfAvailable: StateFlow<Boolean> = _ebpfAvailable.asStateFlow()
    private val _kernels = MutableStateFlow<List<KernelManager.Kernel>>(emptyList())
    private val _kernelBusy = MutableStateFlow(false)
    private val _activeKernelId = MutableStateFlow(KernelManager.BUNDLED_ALPHA_ID)
    private val _installedKernels = MutableStateFlow<List<InstalledKernelUi>>(emptyList())

    data class NetworkSettingsScreenState(
        val runMode: RunMode = RunMode.VpnService,
        val disableAllOverride: Boolean = false,
        val accessControlMode: AccessControlMode = AccessControlMode.ALLOW_ALL,
        val rootAvailable: Boolean = false,
        val ebpfAvailable: Boolean = false,
        val kernels: List<KernelManager.Kernel> = emptyList(),
        val kernelBusy: Boolean = false,
        val activeKernelId: String = KernelManager.BUNDLED_ALPHA_ID,
        val installedKernels: List<InstalledKernelUi> = emptyList(),
    )

    val networkScreenState: StateFlow<NetworkSettingsScreenState> =
        combine(
            combine(
                runMode.state,
                disableAllOverride.state,
                accessControlMode.state,
                rootAvailable,
                ebpfAvailable,
            ) { mode, disableOverride, accessMode, root, ebpf ->
                NetworkSettingsScreenState(
                    runMode = mode,
                    disableAllOverride = disableOverride,
                    accessControlMode = accessMode,
                    rootAvailable = root,
                    ebpfAvailable = ebpf,
                )
            },
            combine(
                _kernels,
                _kernelBusy,
                _activeKernelId,
                _installedKernels,
            ) { availableKernels, busy, active, installed ->
                KernelUiSelection(availableKernels, busy, active, installed)
            },
        ) { base, kernel ->
            base.copy(
                kernels = kernel.kernels,
                kernelBusy = kernel.busy,
                activeKernelId = kernel.active,
                installedKernels = kernel.installed,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                NetworkSettingsScreenState(
                    runMode = runMode.value,
                    disableAllOverride = disableAllOverride.value,
                    accessControlMode = accessControlMode.value,
                    rootAvailable = false,
                    ebpfAvailable = false,
                    kernels = emptyList(),
                    kernelBusy = false,
                    activeKernelId = KernelManager.BUNDLED_ALPHA_ID,
                ),
            )

    data class TunOptionsScreenState(
        val ifName: String = "",
        val mtu: Int = 0,
        val stack: TunStack = TunStack.System,
        val autoRoute: Boolean = false,
        val strictRoute: Boolean = false,
        val autoRedirect: Boolean = false,
        val dnsMode: TunDnsMode = TunDnsMode.FakeIp,
        val enableIPv6: Boolean = false,
    )

    val tunOptionsScreenState: StateFlow<TunOptionsScreenState> =
        combine(
            combine(
                tunIfName.state,
                tunMtu.state,
                tunStack.state,
                tunAutoRoute.state,
                tunStrictRoute.state,
            ) { ifName, mtu, stack, autoRoute, strictRoute ->
                TunOptionsScreenState(
                    ifName = ifName,
                    mtu = mtu,
                    stack = stack,
                    autoRoute = autoRoute,
                    strictRoute = strictRoute,
                )
            },
            combine(tunAutoRedirect.state, tunDnsMode.state, enableIPv6.state) { autoRedirect,
                                                                                 dnsMode,
                                                                                 ipv6 ->
                Triple(autoRedirect, dnsMode, ipv6)
            },
        ) { base, extra ->
            base.copy(
                autoRedirect = extra.first,
                dnsMode = extra.second,
                enableIPv6 = extra.third,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                TunOptionsScreenState(
                    ifName = tunIfName.value,
                    mtu = tunMtu.value,
                    stack = tunStack.value,
                    autoRoute = tunAutoRoute.value,
                    strictRoute = tunStrictRoute.value,
                    autoRedirect = tunAutoRedirect.value,
                    dnsMode = tunDnsMode.value,
                    enableIPv6 = enableIPv6.value,
                ),
            )

    init {
        viewModelScope.launch {
            // Everything below reads the kernel directory; none of it may run on the caller's
            // frame (this constructor runs during composition of the settings page).
            _activeKernelId.value =
                withContext(Dispatchers.IO) { KernelManager.activeKernelId(getApplication()) }
            refreshInstalledKernels()
            _rootAvailable.value = RootAccessSupport.isGranted()
            refreshEbpfAvailability()
        }
    }

    val tunServiceOptionsUiState: StateFlow<TunServiceOptionsUiState> =
        combine(
            bypassPrivateNetwork.state,
            dnsHijack.state,
            enableIPv6.state,
            allowBypass.state,
            systemProxy.state,
        ) { bypassPrivateNetwork, dnsHijack, enableIPv6, allowBypass, systemProxy ->
            TunServiceOptionsUiState(
                common =
                    CommonTunOptionsUiState(
                        bypassPrivateNetwork = bypassPrivateNetwork,
                        dnsHijack = dnsHijack,
                        enableIPv6 = enableIPv6,
                    ),
                allowBypass = allowBypass,
                systemProxy = systemProxy,
            )
        }
            .stateInWhileSubscribed(
                viewModelScope,
                TunServiceOptionsUiState(
                    common =
                        CommonTunOptionsUiState(
                            bypassPrivateNetwork = bypassPrivateNetwork.value,
                            dnsHijack = dnsHijack.value,
                            enableIPv6 = enableIPv6.value,
                        ),
                    allowBypass = allowBypass.value,
                    systemProxy = systemProxy.value,
                ),
            )

    /**
     * Selects the run mode — the single mode key across the runtime. The root Tun cards
     * are disabled in the UI when [rootAvailable] is false, so reaching here for a root mode
     * already implies root was granted.
     */
    fun onRunModeChange(mode: RunMode) {
        if (mode == RunMode.Ebpf && !_ebpfAvailable.value) return
        controller.setRunMode(mode)
    }

    fun onBypassPrivateNetworkChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(bypassPrivateNetwork, enabled)
    }

    fun onDnsHijackChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(dnsHijack, enabled)
    }

    fun onAllowBypassChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(allowBypass, enabled)
    }

    fun onEnableIPv6Change(enabled: Boolean) {
        controller.setAndRestartIfNeeded(enableIPv6, enabled)
    }

    fun onSystemProxyChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(systemProxy, enabled)
    }

    fun onTunStackChange(value: TunStack) = controller.setAndRestartIfNeeded(tunStack, value)

    fun onTunAutoRouteChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunAutoRoute, enabled)

    fun onTunStrictRouteChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunStrictRoute, enabled)

    fun onTunAutoRedirectChange(enabled: Boolean) =
        controller.setAndRestartIfNeeded(tunAutoRedirect, enabled)

    fun onTunDnsModeChange(value: TunDnsMode) = controller.setAndRestartIfNeeded(tunDnsMode, value)

    fun onTunIfNameChange(value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) controller.setAndRestartIfNeeded(tunIfName, trimmed)
    }

    fun onTunMtuChange(value: Int) {
        if (value in 576..9000) controller.setAndRestartIfNeeded(tunMtu, value)
    }

    fun onDisableAllOverrideChange(enabled: Boolean) {
        controller.setAndRestartIfNeeded(disableAllOverride, enabled)
    }

    fun onAccessControlModeChange(mode: AccessControlMode) {
        controller.setAndRestartIfNeeded(accessControlMode, mode)
    }

    /**
     * [runCatching] for suspending work: the plain version also catches [CancellationException],
     * which turns "this screen is gone" into a reported failure and lets the block run on.
     */
    private suspend fun <T> runCatchingCancellable(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Kernel operation failed")
            Result.failure(error)
        }

    fun refreshKernels() {
        if (_kernelBusy.value) return
        viewModelScope.launch {
            _kernelBusy.value = true
            try {
                runCatchingCancellable { KernelManager.fetchIndex() }
                    .onSuccess { index ->
                        _kernels.value = index.kernels
                        _activeKernelId.value =
                            withContext(Dispatchers.IO) {
                                KernelManager.activeKernelId(getApplication())
                            }
                    }
            } finally {
                _kernelBusy.value = false
            }
        }
    }

    fun downloadKernels(ids: Set<String>, onFinished: (Boolean) -> Unit = {}) {
        if (_kernelBusy.value || ids.isEmpty()) return
        val selected = _kernels.value.filter { it.id in ids }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _kernelBusy.value = true
            var failed = false
            try {
                selected.forEach { kernel ->
                    if (!failed) {
                        // Downloads and unpacks the core; must not occupy the main thread.
                        val installed =
                            runCatchingCancellable {
                                withContext(Dispatchers.IO) {
                                    KernelManager.install(getApplication(), kernel)
                                }
                            }
                        failed = installed.isFailure
                    }
                }
                if (!failed) refreshInstalledKernels()
            } finally {
                _kernelBusy.value = false
                onFinished(!failed)
            }
        }
    }

    /**
     * Applies the user's kernel pick. Every branch probes or writes the kernel directory, so the
     * work runs off the caller's frame and [busy][NetworkSettingsScreenState.kernelBusy] gates
     * concurrent picks.
     */
    fun selectKernel(id: String) {
        if (_kernelBusy.value) return
        viewModelScope.launch {
            _kernelBusy.value = true
            try {
                applyKernelSelection(id)
            } finally {
                _kernelBusy.value = false
            }
        }
    }

    private suspend fun applyKernelSelection(id: String) {
        val context = getApplication<Application>()
        val installed =
            withContext(Dispatchers.IO) {
                id == KernelManager.BUNDLED_ALPHA_ID || KernelManager.isInstalled(context, id)
            }
        if (installed) {
            activateKernel(id)
            return
        }
        val kernel = _kernels.value.firstOrNull { it.id == id }
        if (kernel == null) {
            // Neither in the fetched index nor on disk: publish the index so the list offers a
            // kernel that actually exists instead of retrying this pick.
            runCatchingCancellable { KernelManager.fetchIndex() }
                .onSuccess { _kernels.value = it.kernels }
            return
        }
        runCatchingCancellable {
            withContext(Dispatchers.IO) { KernelManager.install(context, kernel) }
        }
            .onSuccess {
                withContext(Dispatchers.IO) { KernelManager.activate(context, kernel.id) }
                refreshInstalledKernels()
                onKernelActivated(kernel.id)
            }
    }

    private suspend fun activateKernel(id: String) {
        val context = getApplication<Application>()
        runCatchingCancellable {
            withContext(Dispatchers.IO) { KernelManager.activate(context, id) }
        }
            .onSuccess { onKernelActivated(id) }
    }

    /** Pure probe: whether an eBPF kernel is installed and running. It never touches the config. */
    private suspend fun refreshEbpfAvailability() {
        _ebpfAvailable.value =
            _rootAvailable.value &&
                withContext(Dispatchers.IO) { KernelManager.isEbpfKernelActive(getApplication()) }
    }

    /**
     * The configured mode is eBPF but nothing can run it: fall back to a mode this device supports.
     * Only ever reached from a kernel change, so merely opening settings cannot rewrite the stored
     * run mode (and restart the runtime) behind the user's back.
     */
    private suspend fun failOverFromEbpf() {
        if (_ebpfAvailable.value || runMode.value != RunMode.Ebpf) return
        controller.setRunMode(if (_rootAvailable.value) RunMode.Tun else RunMode.VpnService)
    }

    private suspend fun onKernelActivated(id: String) {
        val wasEbpf = runMode.value == RunMode.Ebpf
        _activeKernelId.value = id
        refreshEbpfAvailability()
        failOverFromEbpf()
        if (!wasEbpf || _ebpfAvailable.value) controller.requestRestartIfRunning()
    }

    /**
     * Reads what is installed once, off the main thread. This is the only source the UI consults:
     * probing the kernel directory reads (and ELF-scans) every candidate, which must not happen
     * during composition or from a non-suspend handler.
     */
    private suspend fun refreshInstalledKernels() {
        _installedKernels.value =
            withContext(Dispatchers.IO) {
                val context = getApplication<Application>()
                KernelManager.installedKernelIds(context).map { id ->
                    InstalledKernelUi(
                        id = id,
                        name = KernelManager.installedName(context, id),
                        commit = KernelManager.installedCommit(context, id),
                    )
                }
            }
    }
}

/** An installed kernel as the settings list shows it: identity, display name, source commit. */
data class InstalledKernelUi(
    val id: String,
    val name: String,
    val commit: String?,
)

private data class KernelUiSelection(
    val kernels: List<KernelManager.Kernel>,
    val busy: Boolean,
    val active: String,
    val installed: List<InstalledKernelUi>,
)

data class CommonTunOptionsUiState(
    val bypassPrivateNetwork: Boolean = false,
    val dnsHijack: Boolean = false,
    val enableIPv6: Boolean = false,
)

data class TunServiceOptionsUiState(
    val common: CommonTunOptionsUiState = CommonTunOptionsUiState(),
    val allowBypass: Boolean = false,
    val systemProxy: Boolean = false,
)
