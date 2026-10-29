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
 */

package com.github.yumeyucca.yumebox.feature.editor.screen

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Atom
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowLeft
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

/**
 * Opens configuration content in a user-selected external editor and writes the saved file back.
 *
 * The file lives in app-private storage and is shared through the existing [FileProvider].
 * No storage permission is needed: the receiving editor gets a temporary read/write grant for
 * this one content URI. A read-only config is opened with a read grant only.
 */
@Composable
fun ExternalEditorScreen(
    navigator: Navigator,
    title: String = YumeTxt.Editor.Title.Config,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (suspend (String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()

    val file =
        remember(title, language) {
            val directory = File(context.filesDir, EXTERNAL_EDITOR_DIRECTORY).apply { mkdirs() }
            val baseName =
                title.trim()
                    .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                    .trim('_')
                    .take(MAX_FILE_NAME_LENGTH)
                    .ifEmpty { "config" }
            File(directory, "$baseName-${System.currentTimeMillis()}.${language.extension()}")
        }
    val mimeType = remember(language) { language.mimeType() }
    val providerAuthority = remember(context) { "${context.packageName}.fileprovider" }

    var fileReady by remember(file) { mutableStateOf(false) }
    var awaitingReturn by remember(file) { mutableStateOf(false) }
    var launchedOnce by remember(file) { mutableStateOf(false) }
    var isReading by remember(file) { mutableStateOf(false) }
    var lastContent by remember(file) { mutableStateOf(initialContent) }
    var status by remember(file) { mutableStateOf<String?>(null) }
    val latestOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(file, initialContent) {
        withContext(Dispatchers.IO) { file.writeText(initialContent) }
        fileReady = true
    }

    DisposableEffect(file) {
        onDispose { runCatching { file.delete() } }
    }

    val readBack: () -> Unit = {
        if (!isReading) {
            isReading = true
            awaitingReturn = false
            coroutineScope.launch {
                val content = withContext(Dispatchers.IO) {
                    runCatching { file.readText() }.getOrNull()
                }
                if (content == null) {
                    context.toast(YumeTxt.Editor.Message.ExternalReadFailed)
                } else if (content != lastContent) {
                    val save = latestOnSave
                    if (save == null) {
                        lastContent = content
                        status = YumeTxt.Editor.Message.ExternalHint
                    } else {
                        val result = runCatching { save(content) }
                        if (result.isSuccess) {
                            lastContent = content
                            context.toast(YumeTxt.Editor.Message.ExternalSaved)
                            navigator.navigateUp()
                        } else {
                            context.toast(YumeTxt.Editor.Message.SaveFailed)
                        }
                    }
                }
                isReading = false
            }
        }
    }
    val currentReadBack by rememberUpdatedState(readBack)

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            currentReadBack()
        }

    val openExternal: () -> Unit = {
        if (fileReady) {
            val uri =
                runCatching { FileProvider.getUriForFile(context, providerAuthority, file) }
                    .getOrNull()
            if (uri == null) {
                context.toast(YumeTxt.Editor.Message.ExternalReadFailed)
            } else {
                val editable = latestOnSave != null
                val editIntent =
                    Intent(if (editable) Intent.ACTION_EDIT else Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        if (editable) addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        clipData = ClipData.newRawUri(title, uri)
                        putExtra(Intent.EXTRA_TITLE, title)
                    }
                val candidates =
                    listOf(
                        editIntent,
                        Intent(editIntent).setType("text/plain"),
                        Intent(editIntent).setAction(Intent.ACTION_VIEW),
                        Intent(editIntent).setAction(Intent.ACTION_VIEW).setType("text/plain"),
                    )
                val launched =
                    candidates.any { intent ->
                        runCatching {
                            launcher.launch(intent)
                            true
                        }.getOrDefault(false)
                    }
                if (launched) {
                    awaitingReturn = true
                    status = null
                } else {
                    awaitingReturn = false
                    context.toast(YumeTxt.Editor.Message.ExternalNoEditor)
                }
            }
        }
    }

    val currentAwaitingReturn by rememberUpdatedState(awaitingReturn)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && currentAwaitingReturn) {
                    currentReadBack()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(fileReady, launchedOnce) {
        if (fileReady && !launchedOnce) {
            launchedOnce = true
            openExternal()
        }
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Yume.ArrowLeft,
                            contentDescription = YumeTxt.Component.Navigation.Back,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .padding(horizontal = UiDp.dp24, vertical = UiDp.dp32),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
            ) {
                Icon(
                    imageVector = Yume.Atom,
                    contentDescription = null,
                    modifier = Modifier.size(UiDp.dp48),
                    tint = MiuixTheme.colorScheme.primary,
                )
                Text(
                    text = YumeTxt.Editor.Message.ExternalHint,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
                status?.let { message ->
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.primary,
                        style = MiuixTheme.textStyles.footnote1,
                    )
                }
                Button(
                    onClick = openExternal,
                    enabled = fileReady && !isReading,
                ) {
                    Text(YumeTxt.Editor.Action.OpenExternal)
                }
            }
        }
    }
}

private const val EXTERNAL_EDITOR_DIRECTORY = "external_editor"
private const val MAX_FILE_NAME_LENGTH = 48

private fun LanguageScope.extension(): String =
    when (this) {
        LanguageScope.Yaml -> "yaml"
        LanguageScope.JavaScript -> "js"
        LanguageScope.Json -> "json"
        LanguageScope.Text -> "txt"
    }

private fun LanguageScope.mimeType(): String =
    when (this) {
        LanguageScope.Yaml -> "application/x-yaml"
        LanguageScope.JavaScript -> "application/javascript"
        LanguageScope.Json -> "application/json"
        LanguageScope.Text -> "text/plain"
    }
