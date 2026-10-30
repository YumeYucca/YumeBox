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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo

/**
 * 选中分组的唯一可变入口。
 *
 * 页面不再区分"选中项由谁托管":本地保存([rememberSelectedGroupController])和分栏共享
 * ViewModel 的那份([rememberSelectedGroupController] 的托管重载)都只是这个 holder 的两种
 * 构造,调用方只需交出一个 holder,而不是"名字 + 回调"两个参数。
 */
@Stable
class SelectedGroupController internal constructor(
    val name: String?,
    private val onSelect: (String?) -> Unit,
) {
    /** 选中 [name];传 `null` 表示清空选中。 */
    fun select(name: String?) {
        onSelect(name)
    }

    fun selectGroup(group: ProxyGroupInfo) {
        select(group.name)
    }

    fun clear() {
        select(null)
    }
}

/** 页面自己保存选中项(手机分栏、sheet)。 */
@Composable
fun rememberSelectedGroupController(): SelectedGroupController {
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    return rememberSelectedGroupController(name = name, onSelect = { name = it })
}

/** 选中项由调用方托管(分栏 shell 共享 ViewModel)。 */
@Composable
fun rememberSelectedGroupController(
    name: String?,
    onSelect: (String?) -> Unit,
): SelectedGroupController {
    // 回调每次重组都会换实例,用 rememberUpdatedState 兜住,holder 才能只随 name 重建。
    val latestOnSelect by rememberUpdatedState(onSelect)
    return remember(name) {
        SelectedGroupController(name) { next -> latestOnSelect(next) }
    }
}

/** [SelectedGroupController.name] 在 [proxyGroups] 里解析出的分组;纯值,不含回调。 */
@Immutable
data class ProxyGroupSelectionState(
    val selectedGroupName: String?,
    val selectedGroup: ProxyGroupInfo?,
    val displayGroup: ProxyGroupInfo?,
)

@Composable
fun rememberProxyGroupSelectionState(
    proxyGroups: List<ProxyGroupInfo>,
    onRefreshGroup: (String) -> Unit,
    retainLastKnownGroup: Boolean,
    controller: SelectedGroupController,
): ProxyGroupSelectionState {
    val selectedGroupName = controller.name
    val selectedGroupSnapshotState = remember { mutableStateOf<ProxyGroupInfo?>(null) }
    val selectedGroup =
        remember(selectedGroupName, proxyGroups) {
            selectedGroupName?.let { groupName ->
                proxyGroups.firstOrNull { group -> group.name == groupName }
            }
        }
    val displayGroup =
        remember(selectedGroup, selectedGroupSnapshotState.value, retainLastKnownGroup) {
            selectedGroup ?: selectedGroupSnapshotState.value.takeIf { retainLastKnownGroup }
        }

    LaunchedEffect(selectedGroup, retainLastKnownGroup) {
        if (retainLastKnownGroup) {
            selectedGroup?.let { selectedGroupSnapshotState.value = it }
        }
    }

    LaunchedEffect(
        controller,
        selectedGroupName,
        selectedGroup,
        retainLastKnownGroup,
    ) {
        if (!retainLastKnownGroup && selectedGroupName != null && selectedGroup == null) {
            controller.clear()
        }
    }

    LaunchedEffect(selectedGroupName) { selectedGroupName?.let(onRefreshGroup) }

    return remember(selectedGroupName, selectedGroup, displayGroup) {
        ProxyGroupSelectionState(
            selectedGroupName = selectedGroupName,
            selectedGroup = selectedGroup,
            displayGroup = displayGroup,
        )
    }
}
