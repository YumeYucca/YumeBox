/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
fun ProxyShellNodeDetail(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onNavigateToRules: (() -> Unit)? = null,
) = ProxyShellNodeDetailContent(
    mainInnerPadding = mainInnerPadding,
    onNavigateToProviders = onNavigateToProviders,
    onOpenSettings = onOpenSettings,
    onNavigateToRules = onNavigateToRules,
)
