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
    onOpenSettings: (() -> Unit)? = null,
    onOpenRouting: (() -> Unit)? = null,
) = ProxyShellNodeDetailContent(
    mainInnerPadding = mainInnerPadding,
    onOpenSettings = onOpenSettings,
    onOpenRouting = onOpenRouting,
)
