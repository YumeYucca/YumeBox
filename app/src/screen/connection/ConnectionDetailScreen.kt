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

package com.github.yumeyucca.yumebox.screen.connection


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.feature.meta.presentation.component.ConnectionDetailContent
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Full-page connection detail. Replaces the old bottom sheet so search expand / sheet open no
 * longer fight for the same gesture layer.
 */
@Composable
fun ConnectionDetailScreen(navigator: Navigator, connectionId: String) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val mainLikePadding = rememberStandalonePageMainPadding()
    val spacing = AppTheme.spacing
    val scope = rememberCoroutineScope()

    var connection by remember {
        mutableStateOf(ConnectionDetailHolder.connection?.takeIf { it.id == connectionId })
    }
    var canInterrupt by remember {
        mutableStateOf(ConnectionDetailHolder.canInterrupt && connection != null)
    }
    var isInterrupting by remember { mutableStateOf(false) }

    LaunchedEffect(connectionId, canInterrupt) {
        if (!canInterrupt) return@LaunchedEffect
        PollingTimers.ticks(PollingTimerSpecs.ConnectionsPolling).collect {
            val latest =
                withContext(Dispatchers.IO) {
                    runCatching {
                        RuntimeAccess.connect(context)
                        RuntimeAccess.core().queryConnections().connections
                    }
                        .getOrNull()
                }
            val match = latest?.firstOrNull { it.id == connectionId }
            if (match != null) {
                connection = match
                ConnectionDetailHolder.connection = match
            } else if (connection != null) {
                // Connection closed while viewing — freeze the last snapshot and hide interrupt.
                canInterrupt = false
                ConnectionDetailHolder.canInterrupt = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Only clear if this page still owns the staged entry (avoid wiping a newer push).
            if (ConnectionDetailHolder.connection?.id == connectionId) {
                ConnectionDetailHolder.clear()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Connection.Detail.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val combinedPadding = combinePaddingValues(innerPadding, mainLikePadding)
        val info = connection
        if (info == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(combinedPadding)
                    .padding(spacing.space32),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = YumeTxt.Connection.NoResults,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinedPadding,
            ) {
                item(key = "detail") {
                    ConnectionDetailContent(
                        connectionInfo = info,
                        canInterrupt = canInterrupt,
                        isInterrupting = isInterrupting,
                        onInterrupt = {
                            if (isInterrupting) return@ConnectionDetailContent
                            isInterrupting = true
                            scope.launch {
                                val closed =
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            RuntimeAccess.connect(context)
                                            RuntimeAccess.core().closeConnection(info.id)
                                        }
                                            .getOrDefault(false)
                                    }
                                isInterrupting = false
                                if (closed) {
                                    navigator.pop()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Stages the selected `ConnectionInfo` across the navigation boundary. Route only carries the id
 * (serializable); the full snapshot lives here, same pattern as `EditorDataHolder`.
 */
object ConnectionDetailHolder {
    var connection: ConnectionInfo? = null
        internal set

    var canInterrupt: Boolean = false
        internal set

    fun setup(info: ConnectionInfo, canInterrupt: Boolean) {
        connection = info
        this.canInterrupt = canInterrupt
    }

    fun clear() {
        connection = null
        canInterrupt = false
    }
}
