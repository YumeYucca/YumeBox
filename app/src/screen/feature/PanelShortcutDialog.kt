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

package com.github.yumeyucca.yumebox.screen.feature


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.yumeyucca.yumebox.R
import com.github.yumeyucca.yumebox.presentation.component.AppFormDialog
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 创建桌面快捷方式对话框：图标预览、可编辑名称、选图标/恢复默认。 */
@Composable
fun PanelShortcutDialog(
    show: Boolean,
    url: String,
    defaultLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, iconUri: Uri?) -> Unit,
    onDismissFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    val spacing = AppTheme.spacing

    var name by remember(url, defaultLabel) { mutableStateOf(defaultLabel) }
    var customIconUri by remember(url) { mutableStateOf<Uri?>(null) }

    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) customIconUri = uri
        }

    val faviconUrl = remember(url) { faviconUrlOf(url) }
    val iconModel: String = customIconUri?.toString() ?: faviconUrl
    val canConfirm = name.isNotBlank() && url.isNotBlank()

    AppFormDialog(
        show = show,
        title = YumeTxt.Feature.Panel.ShortcutTitle,
        onDismissRequest = onDismiss,
        onConfirm = { if (canConfirm) onConfirm(name.trim(), customIconUri) },
        confirmEnabled = canConfirm,
        scrollable = false,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(vertical = spacing.space16)
                        .size(100.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color.White),
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = FixedScale(1.5f),
                )
                Image(
                    painter = rememberAsyncImagePainter(request = ImageRequest(context, iconModel)),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = YumeTxt.Feature.Panel.ShortcutPickIcon,
                    onClick = { pickIconLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(
                    visible = customIconUri != null,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    IconButton(
                        onClick = { customIconUri = null },
                        modifier = Modifier.padding(start = spacing.space12),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Reset,
                            contentDescription = YumeTxt.Feature.Panel.ShortcutResetIcon,
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            OemTextField(
                value = name,
                onValueChange = { name = it },
                label = YumeTxt.Feature.Panel.ShortcutName,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun faviconUrlOf(url: String): String = runCatching {
    val u = java.net.URL(url)
    "${u.protocol}://${u.host}/favicon.ico"
}
    .getOrDefault(url)
