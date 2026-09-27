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

package com.github.yumeyucca.yumebox.runtime.service.session

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.model.TunConfig
import com.github.yumeyucca.yumebox.data.store.MMKVProvider
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.config.AccessControlMode
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.profile.Imported
import com.github.yumeyucca.yumebox.runtime.service.profile.ImportedDao
import com.github.yumeyucca.yumebox.runtime.service.root.RootPackageShell
import com.github.yumeyucca.yumebox.runtime.service.util.importedDir

class SessionRuntimeSpecFactory(
    context: Context,
    private val store: ServiceStore = ServiceStore(),
) {
    private val context: Context = context.appContextOrSelf
    private val compiledConfigPipeline = CompiledConfigPipeline(this.context)
    private val networkSettings by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings"))
    }

    fun createVpnSpec(): RuntimeSpec = createSpec(RuntimeOwner.VpnService, RunMode.VpnService)

    fun createRootSpec(runMode: RunMode): RuntimeSpec = createSpec(RuntimeOwner.RootDaemon, runMode)

    /** A local, no-TUN core used only to materialize proxy-group state while the app is foregrounded. */
    fun createPreviewSpec(): RuntimeSpec = createSpec(RuntimeOwner.VpnService, RunMode.VpnService, preview = true)

    private fun createSpec(
        owner: RuntimeOwner,
        runMode: RunMode,
        preview: Boolean = false,
    ): RuntimeSpec {
        val profile = requireActiveProfile()
        val profileDir = context.importedDir.resolve(profile.uuid.toString())
        val disableAllUserOverrides = networkSettings.disableAllOverride.value
        val skipModePatches =
            disableAllUserOverrides && runMode == RunMode.Tun
        val skipRuntimePatches = skipModePatches || runMode == RunMode.Ebpf
        val userOverrides =
            if (disableAllUserOverrides) {
                emptyList()
            } else {
                compiledConfigPipeline.resolveOverrideSpecs(profile.uuid.toString())
            }
        val tunConfig =
            if (!skipModePatches && runMode == RunMode.Tun) buildTunConfig() else null
        // Mode/system fragments first; app global-ua always last so it beats subscription + user
        // overrides and survives disable-all (core provider refresh must use the same UA).
        val modeOverrides =
            when {
                tunConfig != null -> userOverrides + TunOverride.materialize(tunConfig, profileDir)
                runMode == RunMode.Ebpf ->
                    userOverrides +
                        listOfNotNull(
                            EbpfOverride.materialize(
                                EbpfOverride.Config(bypassCn = store.ebpfBypassCn),
                                profileDir,
                            )
                        )
                else -> userOverrides
            }
        val overrideSpecs =
            if (runMode == RunMode.Ebpf) modeOverrides
            else modeOverrides + GlobalUaOverride.materialize(profileDir)
        val ageSecretKey = normalizeAgeSecretKey(profile.ageSecretKey)
        return RuntimeSpec(
            owner = owner,
            profileUuid = profile.uuid.toString(),
            profileName = profile.name,
            profileDir = profileDir.absolutePath,
            runtimeConfigPath = profileDir.resolve("runtime.yaml").absolutePath,
            ageSecretKey = ageSecretKey,
            overrideSpecs = overrideSpecs,
            runMode = runMode,
            // eBPF keeps the profile authoritative; Root Tun only skips patches for disable-all.
            skipRuntimePatches = skipRuntimePatches,
            preview = preview,
            tunConfig = tunConfig,
        )
    }

    private fun buildTunConfig(): TunConfig {
        val access = resolveTunAccessControl()
        return TunConfig(
            ifName = networkSettings.tunIfName.value,
            mtu = networkSettings.tunMtu.value,
            stack = networkSettings.tunStack.value.toCoreStack(),
            autoRoute = networkSettings.tunAutoRoute.value,
            strictRoute = networkSettings.tunStrictRoute.value,
            autoRedirect = networkSettings.tunAutoRedirect.value,
            includeUid = access.includeUid,
            excludeUid = access.excludeUid,
            includeAndroidUser = access.includeAndroidUser,
            routeExcludeAddress = networkSettings.tunRouteExcludeAddress.value,
            dnsMode = networkSettings.tunDnsMode.value,
            fakeIpRange = networkSettings.tunFakeIpRange.value,
            fakeIpRange6 = networkSettings.tunFakeIpRange6.value,
            allowIpv6 = networkSettings.enableIPv6.value,
        )
    }

    /**
     * Maps the shared access-control setting onto Tun uid rules. The cmfa build stubs out mihomo's
     * include/exclude-package, so a selected PACKAGE must be resolved to a UID here (include-uid /
     * exclude-uid). All-apps modes fall back to include-android-user (+ the built-in system-uid
     * exclusion in [TunConfig]).
     */
    private fun resolveTunAccessControl(): TunAccessControl {
        val self = context.applicationInfo.uid
        val selectedPackages =
            store.accessControlPackages.map(String::trim).filter(String::isNotEmpty).toSet()
        val rootUidMap = RootPackageShell.queryPackageUidMap(selectedPackages)
        val selectedUid =
            selectedPackages
                .mapNotNull { pkg -> rootUidMap?.get(pkg) ?: resolvePackageUid(pkg) }
                .filter { it != self }
                .distinct()
                .sorted()
        val allUsers = resolveIncludeAndroidUsers()
        return when (store.accessControlMode) {
            AccessControlMode.AcceptAll -> TunAccessControl(includeAndroidUser = allUsers)
            AccessControlMode.AcceptSelected -> TunAccessControl(includeUid = selectedUid)
            AccessControlMode.RejectSelected ->
                TunAccessControl(excludeUid = selectedUid, includeAndroidUser = allUsers)
            // Whitelist only ourselves ⇒ no other app is ever routed into the tun.
            AccessControlMode.RejectAll -> TunAccessControl(includeUid = listOf(self))
        }
    }

    /**
     * Empty list means "all Android users": TunOverride omits include-android-user and sing-tun
     * does not install per-user ExcludeUID ranges. The old hard-coded default [0, 10] only kept
     * owner + work-profile traffic and silently dropped every other multi-user profile.
     */
    private fun resolveIncludeAndroidUsers(): List<Int> {
        val users = networkSettings.tunIncludeAndroidUser.value
        if (users == LEGACY_INCLUDE_ANDROID_USERS) {
            networkSettings.tunIncludeAndroidUser.set(emptyList())
            return emptyList()
        }
        return users
    }

    private fun resolvePackageUid(pkg: String): Int? = runCatching {
        context.packageManager.getPackageInfo(pkg, 0).applicationInfo?.uid
    }.getOrNull()

    private data class TunAccessControl(
        val includeUid: List<Int> = emptyList(),
        val excludeUid: List<Int> = emptyList(),
        val includeAndroidUser: List<Int> = emptyList(),
    )

    private companion object {
        // Pre-fix default that only covered owner + common work-profile id.
        private val LEGACY_INCLUDE_ANDROID_USERS = listOf(0, 10)
    }

    private fun requireActiveProfile(): Imported {
        val profileId = store.activeProfile ?: error("No active profile selected")
        return ImportedDao.queryByUUID(profileId)
            ?: error("Active profile metadata not found: $profileId")
    }

    private fun normalizeAgeSecretKey(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }
}
