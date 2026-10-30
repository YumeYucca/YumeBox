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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.data.model.normalizeProxySheetHeightFraction
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.isSelectable
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.component.FloatingSheetHeaderDefaults
import com.github.yumeyucca.yumebox.presentation.util.isAtTop
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyDelayTestProgress
import kotlinx.coroutines.flow.StateFlow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

// sheet 内的分组/节点列表内边距:库层标题行留空后,内容从卡片顶 + 42dp 开始,
// 再让出 34dp,首行正好落在 76dp 悬浮带的下沿。
// 水平 16dp 是内容自己的内缩——标题带和两侧圆钮是满卡片宽,所以 sheet 的库层 insideMargin
// 水平值已置 0,由这里补上。
val NodeSheetContentPadding =
    PaddingValues(
        start = UiDp.dp16,
        end = UiDp.dp16,
        top = FloatingSheetHeaderDefaults.ContentTopInset,
        bottom = UiDp.dp16,
    )

/**
 * 节点行的唯一点击语义:可选中分组点击即选中;不可选中的分组(自动选择/负载均衡)没有可选
 * 目标,点击改测速。分组页、sheet 页和多列网格共用这一份,不再各写一遍 if。
 */
internal fun nodeClickHandler(
    group: ProxyGroupInfo,
    onSelectProxy: (String) -> Unit,
    onTestDelay: () -> Unit,
): (String) -> Unit =
    { proxyName ->
        if (group.isSelectable) {
            onSelectProxy(proxyName)
        } else {
            onTestDelay()
        }
    }

/**
 * The node-card rows shared by the sheet list and the multi-column page: one card per proxy, keyed by
 * name, with the outer/item padding the caller's layout needs (the grid column passes none of its
 * own, the page passes the horizontal gutter).
 */
internal fun LazyListScope.nodeGridItems(
    proxies: List<Proxy>,
    selectedProxyName: String,
    onProxyClick: ((String) -> Unit)? = null,
    onProxyTest: ((String) -> Unit)? = null,
    testingProxyNames: Set<String> = emptySet(),
    outerHorizontalPadding: Dp = UiDp.dp0,
    itemVerticalPadding: Dp = UiDp.dp0,
) {
    itemsIndexed(
        items = proxies,
        key = { _, proxy -> proxy.name },
        contentType = { _, _ -> "NodeCard1" },
    ) { _, proxy ->
        NodeCard(
            proxy = proxy,
            isSelected = proxy.name == selectedProxyName,
            onClick = onProxyClick,
            onTestClick = onProxyTest,
            isDelayTesting = testingProxyNames.contains(proxy.name),
            showCountryFlag = true,
            modifier =
                Modifier.padding(
                    horizontal = outerHorizontalPadding,
                    vertical = itemVerticalPadding,
                ),
        )
    }
}

/**
 * The one sheet list shape: full width, capped at the sheet fraction, same spacing and padding.
 * Both the group list and the node list are this list, so they cannot drift apart.
 */
@Composable
private fun NodeSheetList(
    sheetHeight: Dp,
    listState: LazyListState,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                // Wrap up to the fraction cap instead of always forcing it: short group/node lists
                // get a content-sized sheet (NexioSchedule semantics), long ones cap and scroll.
                .heightIn(max = sheetHeight)
                .overScrollVertical(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp10),
        contentPadding = NodeSheetContentPadding,
        overscrollEffect = null,
        content = content,
    )
}

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
internal fun rememberNodeSheetHeight(sheetHeightFraction: Float): Dp {
    val normalized = normalizeProxySheetHeightFraction(sheetHeightFraction)
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp, normalized) { screenHeightDp.dp * normalized }
}

@Composable
internal fun NodeGroupSheetContent(
    groups: List<ProxyGroupInfo>,
    testingGroupNames: Set<String>,
    sheetHeightFraction: Float,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupTest: (ProxyGroupInfo) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)

    LaunchedEffect(testingGroupNames) {
        if (testingGroupNames.isNotEmpty() && !listState.isAtTop) {
            listState.animateScrollToItem(0)
        }
    }

    NodeSheetList(sheetHeight = sheetHeight, listState = listState) {
        nodeGroupItems(
            groups = groups,
            onGroupClick = onGroupClick,
            onGroupTest = onGroupTest,
            testingGroupNames = testingGroupNames,
            itemVerticalPadding = UiDp.dp0,
        )
    }
}

@Composable
fun NodeSheetContent(
    group: ProxyGroupInfo,
    onSelectProxy: (String) -> Unit,
    testingGroupNames: Set<String>,
    delayTestProgress: StateFlow<ProxyDelayTestProgress?>,
    testingProxyNames: Set<String> = emptySet(),
    onTestDelay: () -> Unit,
    onTestProxyDelay: (String) -> Unit = {},
    sheetHeightFraction: Float,
    listState: LazyListState = rememberLazyListState(),
) {
    val sheetHeight = rememberNodeSheetHeight(sheetHeightFraction)

    NodeSheetList(sheetHeight = sheetHeight, listState = listState) {
        item(key = "__refresh_indicator__") {
            GroupDelayRefreshIndicator(
                groupName = group.name,
                testingGroupNames = testingGroupNames,
                progress = delayTestProgress,
                textStyle = MiuixTheme.textStyles.footnote1,
            )
        }
        nodeGridItems(
            proxies = group.proxies,
            selectedProxyName = group.now,
            onProxyClick = nodeClickHandler(group, onSelectProxy, onTestDelay),
            onProxyTest = onTestProxyDelay,
            testingProxyNames = testingProxyNames,
        )
    }
}
