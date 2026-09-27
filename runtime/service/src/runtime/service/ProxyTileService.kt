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

package com.github.yumeyucca.yumebox.runtime.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.data.store.MMKVProvider
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeStartSource
import com.github.yumeyucca.yumebox.runtime.api.RuntimeState
import com.github.yumeyucca.yumebox.runtime.api.VpnPermissionRequired
import com.github.yumeyucca.yumebox.runtime.service.profile.ProfileService
import com.github.yumeyucca.yumebox.runtime.service.util.ServiceLogoIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tf.gal.yumebox.locale.YumeTxt
import timber.log.Timber

@SuppressLint("NewApi")
class ProxyTileService : TileService() {
    private val profileManager by lazy { ProfileService(applicationContext) }
    private val networkSettingsStorage by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings"))
    }
    private val tileLabelText: String by lazy {
        applicationInfo.loadLabel(packageManager).toString().ifBlank { "YumeBox" }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var updateJob: Job? = null
    private var toggleJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateJob?.cancel()
        updateJob = scope.launch { RuntimeCoordinator.state.collect(::render) }
    }

    override fun onStopListening() {
        super.onStopListening()
        updateJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val state = RuntimeCoordinator.state.value
        // A second tap while a start is still coming up cancels it.
        if (toggleJob?.isActive == true && state.phase != RuntimePhase.Starting) return

        toggleJob = scope.launch {
            if (RemoteControllerStore.isActive()) {
                showTile(active = true, YumeTxt.Service.Tile.ClickToStopProxy)
                return@launch
            }
            try {
                if (state.phase.isActiveOrStopping) {
                    AutoStartSessionGate.markManualPaused()
                    showTile(active = true, YumeTxt.Service.Tile.Disconnecting)
                    RuntimeCoordinator.stop()
                } else {
                    startFromTile()
                }
            } catch (error: VpnPermissionRequired) {
                showTile(active = false, YumeTxt.Service.Tile.ClickToOpen)
                error.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityAndCollapseCompat(error.intent, requestCode = 1002)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                // fault barrier: the tile must fall back to the real state instead of crashing
                // the SystemUI-bound service.
                Timber.e(error, "Error toggling proxy from tile")
            } finally {
                render(RuntimeCoordinator.state.value)
            }
        }
    }

    private suspend fun startFromTile() {
        val activeProfile = withContext(Dispatchers.IO) { profileManager.queryActive() }
        if (activeProfile == null) {
            showTile(active = false, YumeTxt.Service.Tile.ClickToOpen)
            val intent =
                Intent(Intent.ACTION_MAIN).apply {
                    component = Components.MAIN_ACTIVITY
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            startActivityAndCollapseCompat(intent, requestCode = 1001)
            return
        }
        showTile(active = false, YumeTxt.Service.Tile.Connecting)
        RuntimeCoordinator.start(networkSettingsStorage.runMode.value, RuntimeStartSource.Tile)
    }

    private fun render(state: RuntimeState) {
        when (state.phase) {
            RuntimePhase.Starting -> showTile(active = false, YumeTxt.Service.Tile.Connecting)
            RuntimePhase.Stopping -> showTile(active = true, YumeTxt.Service.Tile.Disconnecting)
            RuntimePhase.Running -> showTile(active = true, YumeTxt.Service.Tile.ClickToStopProxy)
            RuntimePhase.Idle,
            RuntimePhase.Failed -> showTile(active = false, YumeTxt.Service.Tile.ClickToStartProxy)
        }
    }

    private fun showTile(active: Boolean, subtitle: String) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = tileLabelText
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.icon = Icon.createWithResource(this, ServiceLogoIcons.resId())
        tile.updateTile()
    }

    private fun startActivityAndCollapseCompat(intent: Intent, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntentFlags =
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent =
                PendingIntent.getActivity(this, requestCode, intent, pendingIntentFlags)
            startActivityAndCollapse(pendingIntent)
            return
        }

        @Suppress("DEPRECATION")
        @SuppressLint("StartActivityAndCollapseDeprecated")
        startActivityAndCollapse(intent)
    }
}
