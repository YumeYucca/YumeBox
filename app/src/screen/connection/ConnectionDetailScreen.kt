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


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.feature.meta.presentation.component.ConnectionDetailContent
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.ConnectionDetailViewModel
import com.github.yumeyucca.yumebox.presentation.component.*
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/**
 * Full-page connection detail. Replaces the old bottom sheet so search expand / sheet open no
 * longer fight for the same gesture layer.
 */
@Composable
fun ConnectionDetailScreen(
    navigator: Navigator,
    connectionId: String,
    canInterrupt: Boolean,
) {
    val viewModel = koinViewModel<ConnectionDetailViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollBehavior = MiuixScrollBehavior()
    // Read once per id: the staged copy only exists to fill the first frame.
    val staged = remember(connectionId) {
        ConnectionDetailHolder.connection?.takeIf { it.id == connectionId }
    }

    LaunchedEffect(connectionId, canInterrupt) { viewModel.start(connectionId, canInterrupt, staged) }

    LaunchedEffect(Unit) {
        viewModel.closed.collect { navigator.pop() }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
            // Only clear if this page still owns the staged entry (avoid wiping a newer push).
            if (ConnectionDetailHolder.connection?.id == connectionId) {
                ConnectionDetailHolder.clear()
            }
        }
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Connection.Detail.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val combinedPadding = standalonePageInnerPadding(innerPadding)
        val info = state.info
        if (info == null && !state.hasLoaded) {
            // The first poll is still out; "no connections" would be a lie until it answers.
            Spacer(modifier = Modifier.fillMaxSize())
        } else if (info == null) {
            ConnectionEmptyText(
                text = YumeTxt.Connection.NoResults,
                modifier = Modifier.fillMaxSize().padding(combinedPadding),
            )
        } else {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinedPadding,
            ) {
                item(key = "detail") {
                    ConnectionDetailContent(
                        connectionInfo = info,
                        canInterrupt = state.canInterrupt,
                        isInterrupting = state.isInterrupting,
                        onInterrupt = viewModel::interrupt,
                    )
                }
            }
        }
    }
}

/**
 * Stages the selected `ConnectionInfo` across the navigation boundary. Route only carries the id
 * (serializable); the full snapshot lives here.
 */
object ConnectionDetailHolder {
    var connection: ConnectionInfo? = null
        internal set

    fun setup(info: ConnectionInfo) {
        connection = info
    }

    fun clear() {
        connection = null
    }
}
