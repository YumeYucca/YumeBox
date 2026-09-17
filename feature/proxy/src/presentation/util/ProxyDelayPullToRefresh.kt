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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */
@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.presentation.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

@Composable
internal fun rememberAllGroupsPullToRefreshTexts(): List<String> =
    remember {
        listOf(
            YumeTxt.Proxy.PullToRefresh.PullToTestAllGroups,
            YumeTxt.Proxy.PullToRefresh.ReleaseToTestAllGroups,
            YumeTxt.Proxy.PullToRefresh.TestingAllGroups,
            YumeTxt.Proxy.Testing.RequestSent,
        )
    }

@Composable
internal fun rememberCurrentGroupPullToRefreshTexts(): List<String> =
    remember {
        listOf(
            YumeTxt.Proxy.PullToRefresh.PullToTestCurrentGroup,
            YumeTxt.Proxy.PullToRefresh.ReleaseToTestCurrentGroup,
            YumeTxt.Proxy.PullToRefresh.TestingCurrentGroup,
            YumeTxt.Proxy.Testing.RequestSent,
        )
    }

@Composable
internal fun ProxyDelayPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    refreshTexts: List<String>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = { if (!isRefreshing) onRefresh() },
        modifier = modifier,
        pullToRefreshState = rememberPullToRefreshState(),
        contentPadding = contentPadding,
        topAppBarScrollBehavior = scrollBehavior,
        refreshTexts = refreshTexts,
        content = content,
    )
}
