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

package com.github.yumeyucca.yumebox.substore

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.github.yumeyucca.yumebox.substore.engine.NativeLibraryManager
import timber.log.Timber

class SubStoreService : Service() {
    private var caseEngine: CaseEngine? = null
    private var activeRequest: SubStoreServiceRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = SubStoreServiceController.requestFrom(intent)
        if (caseEngine != null && activeRequest == request) {
            SubStoreServiceController.markRunning()
            return START_STICKY
        }
        if (caseEngine != null) {
            cleanupService()
        }

        return runCatching {
            if (
                NetworkUtil.isPortInUse(request.frontendPort) ||
                NetworkUtil.isPortInUse(request.backendPort)
            ) {
                error("Port ${request.frontendPort} or ${request.backendPort} is already in use")
            }

            if (!ensureJavetLibraryLoaded()) {
                error("Failed to load the Javet native library")
            }

            val engine =
                CaseEngine(
                    backendPort = request.backendPort,
                    frontendPort = request.frontendPort,
                    allowLan = request.allowLan,
                    onTerminated = { terminated ->
                        mainHandler.post { handleEngineTermination(terminated, startId) }
                    },
                )
            caseEngine = engine

            if (!engine.isInitialized()) {
                error("Failed to initialize CaseEngine")
            }

            engine.startServer()
            activeRequest = request
            SubStoreServiceController.markRunning()

            START_STICKY
        }
            .getOrElse { error ->
                Timber.e(error, "Sub-Store service start failed")
                cleanupService()
                stopSelf(startId)
                START_NOT_STICKY
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupService()
    }

    private fun ensureJavetLibraryLoaded(): Boolean = runCatching {
        NativeLibraryManager.initialize(applicationContext)

        if (!NativeLibraryManager.isLibraryAvailable()) {
            Timber.e("Javet library is not downloaded")
            return false
        }

        val loaded = NativeLibraryManager.loadJniLibrary()
        if (!loaded) {
            Timber.e("Javet load failed: ${NativeLibraryManager.getLibraryStatus()}")
        }
        loaded
    }
        .getOrElse { error ->
            Timber.e(error, "Javet load error")
            false
        }

    private fun handleEngineTermination(engine: CaseEngine, startId: Int) {
        if (caseEngine !== engine) return
        resetState()
        stopSelfResult(startId)
    }

    private fun cleanupService() {
        runCatching { caseEngine?.stopServer() }
            .onFailure { error -> Timber.e(error, "Failed to stop Sub-Store service") }
        resetState()
    }

    private fun resetState() {
        caseEngine = null
        activeRequest = null
        SubStoreServiceController.markStopped()
    }
}
