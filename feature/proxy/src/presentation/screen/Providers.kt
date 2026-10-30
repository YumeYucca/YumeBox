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

@file:Suppress("FunctionName", "KotlinConstantConditions")

package com.github.yumeyucca.yumebox.presentation.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.formatDateTime
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.Provider
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProvidersViewModel
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.viewmodel.providerKey
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun Provider.VehicleType.localizedDisplayName(): String =
    when (this) {
        Provider.VehicleType.HTTP -> YumeTxt.Providers.VehicleType.Http
        Provider.VehicleType.File -> YumeTxt.Providers.VehicleType.File
        Provider.VehicleType.Inline -> YumeTxt.Providers.VehicleType.Inline
        Provider.VehicleType.Compatible -> YumeTxt.Providers.VehicleType.Compatible
    }

private data class ProviderSection(
    val title: String,
    val providers: List<Provider>,
)

@OptIn(ExperimentalFoundationApi::class)
@Suppress("KotlinConstantConditions")
@Composable
fun ProvidersContent(
    contentPadding: PaddingValues = PaddingValues(),
    scrollBehaviorOverride: ScrollBehavior? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    val viewModel = koinViewModel<ProvidersViewModel>()
    val scrollBehavior = scrollBehaviorOverride ?: MiuixScrollBehavior()
    val context = LocalContext.current
    // One launcher for the whole list; a launcher per card registers N activity results on entry.
    var pendingUploadProvider by remember { mutableStateOf<Provider?>(null) }
    val filePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            val provider = pendingUploadProvider
            pendingUploadProvider = null
            if (uri != null && provider != null) {
                viewModel.uploadProviderFile(provider, uri)
            }
        }
    val requestUpload: (Provider) -> Unit = { provider ->
        pendingUploadProvider = provider
        filePicker.launch("*/*")
    }

    val providers by viewModel.providers.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isRunning) {
        if (isRunning) {
            viewModel.refreshProviders()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            context.toast(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            context.toast(it, Toast.LENGTH_LONG)
            viewModel.clearError()
        }
    }

    val sections =
        remember(providers) {
            val (proxyProviders, ruleProviders) =
                providers.partition { it.type == Provider.Type.Proxy }
            buildList {
                if (proxyProviders.isNotEmpty()) {
                    add(
                        ProviderSection(
                            title =
                                YumeTxt.Providers.Type.ProxyProviders.format(proxyProviders.size),
                            providers = proxyProviders,
                        )
                    )
                }
                if (ruleProviders.isNotEmpty()) {
                    add(
                        ProviderSection(
                            title = YumeTxt.Providers.Type.RuleProviders.format(ruleProviders.size),
                            providers = ruleProviders,
                        )
                    )
                }
            }
        }

    var selectedSectionIndex by rememberSaveable { mutableStateOf(0) }

    if (!isRunning) {
        CenteredText(
            firstLine = YumeTxt.Providers.Empty.NotRunning,
            secondLine = YumeTxt.Providers.Empty.NotRunningHint,
        )
    } else if (providers.isEmpty() && !uiState.isLoading) {
        CenteredText(
            firstLine = YumeTxt.Providers.Empty.NoProviders,
            secondLine = YumeTxt.Providers.Empty.NoProvidersHint,
        )
    } else {
        val showProviderTabs = sections.size >= 2
        val activeSectionIndex =
            if (showProviderTabs) selectedSectionIndex.coerceIn(0, sections.lastIndex) else 0
        // A provider refresh can drop a section; keep the stored index inside the list instead of
        // leaving a value that only looks right while it is being read.
        LaunchedEffect(sections.size, activeSectionIndex) {
            if (showProviderTabs && selectedSectionIndex != activeSectionIndex) {
                selectedSectionIndex = activeSectionIndex
            }
        }
        // The tab row already names the visible section, so that section drops its own header.
        val visibleSections = if (showProviderTabs) listOf(sections[activeSectionIndex]) else sections
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = contentPadding,
            lazyListState = listState,
        ) {
            if (showProviderTabs) {
                stickySegmentedTabs(
                    key = "__provider_tabs__",
                    tabs = sections.map { it.title },
                    selectedTabIndex = activeSectionIndex,
                    onTabSelected = { selectedSectionIndex = it },
                )
            }
            visibleSections.forEach { section ->
                providerSection(
                    section = section,
                    showTitle = !showProviderTabs,
                    isUpdating = { key -> uiState.updatingProviders.contains(key) },
                    onUpdate = { provider -> viewModel.updateProvider(provider) },
                    onUpload = requestUpload,
                )
            }
        }
    }
}

@Suppress("KotlinConstantConditions")
@Composable
private fun ProviderCard(
    provider: Provider,
    isUpdating: Boolean,
    onUpdate: () -> Unit,
    onUpload: () -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }
    val colorScheme = MiuixTheme.colorScheme
    val updateBg = remember(colorScheme) { colorScheme.primary.copy(alpha = 0.1f) }
    val updateTint = remember(colorScheme) { colorScheme.primary }
    val formattedUpdatedAt = remember(provider.updatedAt) { formatTimestamp(provider.updatedAt) }

    AppCard(modifier = Modifier.padding(vertical = UiDp.dp4)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiDp.dp16, vertical = UiDp.dp12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.size(UiDp.dp4))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = provider.vehicleType.localizedDisplayName(),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (provider.updatedAt > 0) {
                        Text(
                            text = "•",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = formattedUpdatedAt,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(UiDp.dp8))

            // REST list payloads do not include path (website/api/openapi.json). Gate by
            // vehicleType instead: HTTP always updates via PUT; File can upload only when
            // path is known (local import path). Inline/Compatible have no actions.
            val canUpdate =
                provider.vehicleType == Provider.VehicleType.HTTP ||
                        provider.vehicleType == Provider.VehicleType.File
            val canUpload =
                provider.vehicleType == Provider.VehicleType.File && provider.path.isNotBlank()

            if (canUpdate || canUpload) {
                // Update and upload share one control: a provider that offers only one of them
                // fires it straight away, one that offers both grows this menu out of the button.
                val hasActionMenu = canUpdate && canUpload
                val actionAnchor = rememberGlassPopupAnchor()
                Box {
                    IconButton(
                        modifier =
                            Modifier.glassPopupAnchor(
                                anchor = actionAnchor,
                                cornerRadius = UiDp.dp35 / 2,
                                floating = false,
                            ),
                        backgroundColor = updateBg,
                        minHeight = UiDp.dp35,
                        minWidth = UiDp.dp35,
                        enabled = !isUpdating,
                        onClick = {
                            when {
                                hasActionMenu -> showPopup.value = true
                                canUpdate -> onUpdate()
                                else -> onUpload()
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = UiDp.dp10),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                        ) {
                            Icon(
                                modifier =
                                    Modifier
                                        .size(UiDp.dp20)
                                        .glassPopupAnchorContent(actionAnchor),
                                imageVector = MiuixIcons.Edit,
                                tint = updateTint,
                                contentDescription = YumeTxt.Providers.Action.Operation,
                            )
                            Text(
                                modifier = Modifier.padding(end = UiDp.dp3),
                                text =
                                    if (hasActionMenu) {
                                        YumeTxt.Providers.Action.Operation
                                    } else if (canUpdate) {
                                        YumeTxt.Providers.Action.Update
                                    } else {
                                        YumeTxt.Providers.Action.Upload
                                    },
                                color = updateTint,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                            )
                        }
                    }

                    if (hasActionMenu) {
                        GlassPopupHost(
                            show = showPopup.value,
                            onDismissRequest = { showPopup.value = false },
                            anchor = actionAnchor,
                            anchorContent = { GlassTopActionAnchorIcon(MiuixIcons.Edit) },
                        ) {
                            GlassMenuItem(
                                text = YumeTxt.Providers.Action.Update,
                                onClick = {
                                    showPopup.value = false
                                    onUpdate()
                                },
                            )
                            GlassMenuItem(
                                text = YumeTxt.Providers.Action.Upload,
                                onClick = {
                                    showPopup.value = false
                                    onUpload()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.providerSection(
    section: ProviderSection,
    isUpdating: (String) -> Boolean,
    onUpdate: (Provider) -> Unit,
    onUpload: (Provider) -> Unit,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        item(key = "title_${section.title}") { Title(section.title) }
    }
    items(
        items = section.providers,
        key = { provider -> providerKey(provider) },
        contentType = { "ProviderCard" },
    ) { provider ->
        ProviderCard(
            provider = provider,
            isUpdating = isUpdating(providerKey(provider)),
            onUpdate = { onUpdate(provider) },
            onUpload = { onUpload(provider) },
        )
    }
}

private fun formatTimestamp(ts: Long): String = formatDateTime(ts, "MM-dd HH:mm")
