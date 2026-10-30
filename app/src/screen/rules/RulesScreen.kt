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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox.screen.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.RuntimeRule
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Runtime rules from `GET /rules`. Each card has an enable switch wired to `PATCH /rules/disable`.
 * This is **not** the custom-routing editor.
 *
 * Rendered inline inside the shared scaffold owned by [RulesAndProvidersScreen]: this composable
 * owns no top bar, search overlay or tab band of its own. Horizontal inset for cards is owned by
 * shared [AppCard] (`applyHorizontalPadding`), same as Log — not LazyColumn contentPadding.
 */
@Composable
fun RulesScreen(
    contentPadding: PaddingValues = PaddingValues(),
    scrollBehaviorOverride: ScrollBehavior? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    val viewModel = koinViewModel<RulesViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = scrollBehaviorOverride ?: MiuixScrollBehavior()

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(uiState.toggleError) {
        val error = uiState.toggleError ?: return@LaunchedEffect
        context.toast(YumeTxt.Rules.Message.ToggleFailed.replace("%s", error))
        viewModel.consumeToggleError()
    }
    when {
        uiState.isLoading && uiState.rules.isEmpty() -> {
            CenteredText(
                firstLine = YumeTxt.Rules.Empty.Loading,
                secondLine = YumeTxt.Rules.Empty.LoadingHint,
            )
        }

        !uiState.isRunning && uiState.rules.isEmpty() -> {
            CenteredText(
                firstLine = YumeTxt.Rules.Empty.NotRunning,
                secondLine = YumeTxt.Rules.Empty.NotRunningHint,
            )
        }

        uiState.rules.isEmpty() -> {
            CenteredText(
                firstLine = YumeTxt.Rules.Empty.NoRules,
                secondLine = YumeTxt.Rules.Empty.NoRulesHint,
            )
        }

        else -> {
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = contentPadding,
                lazyListState = listState,
            ) {
                items(
                    items = uiState.rules,
                    key = { it.index },
                    contentType = { "rule" },
                ) { rule ->
                    RuleCard(
                        rule = rule,
                        enabled = !rule.disabled,
                        toggling = rule.index in uiState.togglingIndexes,
                        onEnabledChange = { enabled ->
                            viewModel.setRuleEnabled(rule.index, enabled)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: RuntimeRule,
    enabled: Boolean,
    toggling: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val spacing = AppTheme.spacing
    val locale = LocalLocale.current.platformLocale
    val totalCount = rule.hitCount + rule.missCount
    val hitRate =
        if (totalCount > 0L) {
            String.format(locale, "%.1f%%", rule.hitCount * 100.0 / totalCount)
        } else {
            "-"
        }
    AppCard(modifier = Modifier.padding(vertical = spacing.space4)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space16, vertical = spacing.space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                Text(
                    text = rule.payload.ifBlank { "#" + rule.index },
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !toggling,
                )
            }
            Spacer(modifier = Modifier.size(spacing.space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                Text(
                    text = "${rule.type.ifBlank { "-" }}  ·  " + rule.proxy.ifBlank { "-" },
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = YumeTxt.Rules.Label.HitRate.replace("%s", hitRate),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
        }
    }
}
