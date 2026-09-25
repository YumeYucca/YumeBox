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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.screen.node


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.rememberRowShown
import com.github.yumeyucca.yumebox.presentation.theme.rowReveal
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

internal fun LazyListScope.nodeGridItems(
    proxies: List<Proxy>,
    selectedProxyName: String,
    onProxyClick: ((String) -> Unit)? = null,
    onProxyTest: ((String) -> Unit)? = null,
    testingProxyNames: Set<String> = emptySet(),
    outerHorizontalPadding: Dp = UiDp.dp0,
    itemVerticalPadding: Dp = UiDp.dp0,
    revealCount: Int = Int.MAX_VALUE,
) {
    itemsIndexed(
        items = proxies,
        key = { _, proxy -> proxy.name },
        contentType = { _, _ -> "NodeCard1" },
    ) { index, proxy ->
        NodeCard(
            proxy = proxy,
            isSelected = proxy.name == selectedProxyName,
            onClick = onProxyClick,
            onTestClick = onProxyTest,
            isDelayTesting = testingProxyNames.contains(proxy.name),
            showCountryFlag = true,
            modifier =
                Modifier
                    .rowReveal(rememberRowShown(index, revealCount))
                    .padding(
                        horizontal = outerHorizontalPadding,
                        vertical = itemVerticalPadding,
                    ),
        )
    }
}

@Composable
internal fun NodeGrid(
    proxies: List<Proxy>,
    selectedProxyName: String,
    onProxyClick: ((String) -> Unit)? = null,
    onProxyTest: ((String) -> Unit)? = null,
    testingProxyNames: Set<String> = emptySet(),
    listStateKey: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(UiDp.dp0),
) {
    val listState = rememberSaveable(listStateKey, saver = LazyListState.Saver) { LazyListState() }
    LazyColumn(
        modifier = modifier
            .scrollEndHaptic()
            .overScrollVertical(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        overscrollEffect = null,
    ) {
        nodeGridItems(
            proxies = proxies,
            selectedProxyName = selectedProxyName,
            onProxyClick = onProxyClick,
            onProxyTest = onProxyTest,
            testingProxyNames = testingProxyNames,
        )
    }
}
