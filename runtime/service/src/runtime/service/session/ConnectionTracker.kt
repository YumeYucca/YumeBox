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
import com.github.yumeyucca.yumebox.core.domain.ConnectionHistoryManager
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Feeds [ConnectionHistoryManager] from `/connections`, but only while an activity is visible:
 * nothing reads the history in the background, so polling there only wakes the core.
 */
internal class ConnectionTracker(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        stop()
        job =
            scope.launch(Dispatchers.IO) {
                AppVisibilityTracker.isForeground.collectLatest { foreground ->
                    if (!foreground) return@collectLatest
                    PollingTimers.ticks(PollingTimerSpecs.SessionConnectionTracking).collect {
                        runCatching {
                            val snapshot = CoreProcess.controller(context).queryConnections()
                            ConnectionHistoryManager.updateConnections(snapshot.connections)
                        }
                    }
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
