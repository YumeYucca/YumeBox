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

package com.github.yumeyucca.yumebox.presentation.component.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import java.util.Locale
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val MarkdownBaseUrl = "https://github.com/YumeYucca/YumeBox/"

private val markdownExtensions = listOf(
    TablesExtension.create(),
    StrikethroughExtension.builder().requireTwoTildes(true).build(),
    AutolinkExtension.create(),
    TaskListItemsExtension.create(),
)

private val markdownParser = Parser.builder()
    .extensions(markdownExtensions)
    .build()

private val markdownRenderer = HtmlRenderer.builder()
    .extensions(markdownExtensions)
    .escapeHtml(true)
    .sanitizeUrls(true)
    .build()

/**
 * Renders GitHub-flavored Markdown in a WebView.
 *
 * Adapted from KernelSU manager's `MarkdownContent` / `GithubMarkdown`
 * (https://github.com/tiann/KernelSU, GPL-3.0), trimmed to the update-dialog use case:
 * no JavaScript bridge, no remote asset loader, and all links leave the app. JavaScript itself is
 * enabled only to read the rendered body height; the Markdown source is escaped and URL-sanitized,
 * so the page cannot inject scripts.
 *
 * Until the page reports its rendered height a refresh indicator fills the body, so the caller can
 * start with a compact placeholder instead of a max-height gap.
 */
@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    onContentHeight: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val systemDensity = LocalResources.current.displayMetrics.density
    val textZoom = (100 * (density.density / systemDensity) * density.fontScale)
        .toInt()
        .coerceIn(50, 200)
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "rtl" else "ltr"
    val scheme = MiuixTheme.colorScheme

    val css = remember(context) { readAsset(context, "webview/markdown.css") }
    val variables = buildString {
        append(":root{")
        append("--fg:").append(scheme.onSurface.cssHex()).append(';')
        append("--fg-muted:").append(scheme.onSurfaceVariantSummary.cssHex()).append(';')
        append("--link:").append(scheme.primary.cssHex()).append(';')
        append("--code-bg:").append(scheme.surfaceVariant.cssHex()).append(';')
        append("--border:").append(scheme.outline.cssHex()).append(';')
        append("--table-alt:").append(scheme.surfaceVariant.cssHex()).append(';')
        append('}')
    }
    val html = remember(content, css, variables, direction) {
        val body = markdownRenderer.render(markdownParser.parse(content))
        """
        <!DOCTYPE html>
        <html dir="$direction">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>$css</style>
          <style>$variables</style>
        </head>
        <body><main class="markdown-body">$body</main></body>
        </html>
        """.trimIndent()
    }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(html) { loaded = false }
    val currentOnContentHeight = rememberUpdatedState(onContentHeight)

    Box(modifier = modifier) {
        AndroidView(
            factory = { viewContext ->
                createWebView(viewContext, textZoom) { height ->
                    loaded = true
                    if (height > 0) currentOnContentHeight.value?.invoke(height)
                }
            },
            update = { webView ->
                webView.settings.textZoom = textZoom
                if (webView.tag != html) {
                    webView.tag = html
                    webView.loadDataWithBaseURL(MarkdownBaseUrl, html, "text/html", "utf-8", null)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp24))
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: Context,
    zoomPercent: Int,
    onMeasured: (Int) -> Unit,
): WebView = WebView(context).apply {
    setBackgroundColor(AndroidColor.TRANSPARENT)
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = false
        allowFileAccess = false
        allowContentAccess = false
        loadsImagesAutomatically = true
        blockNetworkImage = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        textZoom = zoomPercent
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }

    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val scheme = uri.scheme?.lowercase(Locale.US)
            if (scheme == "http" || scheme == "https") {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            // Measure the rendered body, not the scroll range: a document shorter than the view
            // still reports the full viewport as its scroll range, which left the dialog at its
            // max height with an empty gap.
            view.post {
                view.evaluateJavascript(ContentHeightScript) { result ->
                    val height = result?.trim()?.toDoubleOrNull()?.toInt() ?: 0
                    onMeasured(height)
                }
            }
        }
    }
}

/**
 * Returns the rendered Markdown body height in physical pixels. `devicePixelRatio` matches the
 * display density, so the result can be converted back with `LocalDensity`.
 */
private const val ContentHeightScript =
    "(function(){var e=document.querySelector('.markdown-body');" +
        "return e?Math.ceil(e.getBoundingClientRect().height*window.devicePixelRatio):0})()"

private fun readAsset(context: Context, path: String): String =
    runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrDefault("")

private fun Color.cssHex(): String =
    String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)
