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

package com.github.yumeyucca.yumebox.runtime.client.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.github.yumeyucca.yumebox.runtime.api.Intents
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import timber.log.Timber

/**
 * Forwards profile / override edits to the session. Runtime lifecycle arrives through the
 * control's state flow, not broadcasts.
 */
internal class RuntimeEventBridge(
    context: Context,
    private val onConfigChanged: () -> Unit,
) {
    private val appContext = context.appContextOrSelf
    private val packageName = appContext.packageName

    private val actionProfileChanged = Intents.actionProfileChanged(packageName)
    private val actionOverrideChanged = Intents.actionOverrideChanged(packageName)

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action ?: return) {
                    actionProfileChanged -> {
                        if (intent.getBooleanExtra(Intents.EXTRA_AFFECTS_RUNTIME, true)) {
                            onConfigChanged()
                        }
                    }

                    actionOverrideChanged -> onConfigChanged()
                }
            }
        }

    fun register() {
        val filter =
            IntentFilter().apply {
                addAction(actionProfileChanged)
                addAction(actionOverrideChanged)
            }
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
            .onFailure { error -> Timber.w(error, "Failed to register service event receiver") }
    }
}
