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

package com.github.yumeyucca.yumebox

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.presentation.webview.WebViewScreen
import com.github.yumeyucca.yumebox.core.locale.ProvideYumeStrings
import timber.log.Timber

class WebViewActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_INITIAL_URL = "initial_url"

        fun intent(context: Context, initialUrl: String): Intent =
            Intent(context, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_INITIAL_URL, initialUrl)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun start(
            context: Context,
            initialUrl: String = "file://${context.filesDir}/frontend/index.html",
        ) {
            context.startActivity(intent(context, initialUrl))
        }
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            filePathCallback?.onReceiveValue(uris.toTypedArray())
            filePathCallback = null
        }

    fun launchFilePicker(callback: ValueCallback<Array<Uri>>?, mimeTypes: Array<String>) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback
        val types =
            if (mimeTypes.isEmpty() || (mimeTypes.size == 1 && mimeTypes[0].isEmpty())) {
                arrayOf("*/*")
            } else {
                mimeTypes
            }
        fileChooserLauncher.launch(types)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isDarkMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        val useLightNavigationBarIcons = !isDarkMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (useLightNavigationBarIcons) {
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                } else {
                    0
                },
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightNavigationBars = useLightNavigationBarIcons
        }

        val initialUrl =
            intent.getStringExtra(EXTRA_INITIAL_URL).toLoadableUrl()
                ?: "file://$filesDir/frontend/index.html"

        setContent { ProvideYumeStrings { WebViewScreen(initialUrl = initialUrl) } }
    }

    // This activity is exported so pinned shortcuts can reopen it, which means any installed app
    // can hand it a URL. Only web links and the bundled frontend document may be loaded: anything
    // else (another file:// path, content://, javascript:) would run inside this app's WebView with
    // its privileges.
    private fun String?.toLoadableUrl(): String? {
        val url = this?.trim().orEmpty()
        if (url.isEmpty()) return null
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme == "http" || scheme == "https") return url
        if (scheme == "file") {
            val uri = Uri.parse(url)
            val path = uri.path ?: return null
            val insideFrontend = runCatching {
                val root = java.io.File(filesDir, "frontend").canonicalFile.toPath()
                java.io.File(path).canonicalFile.toPath().startsWith(root)
            }.getOrDefault(false)
            if (uri.authority.isNullOrEmpty() && insideFrontend) return url
        }
        Timber.w("Refusing to load unsupported URL from an external intent: %s", url)
        return null
    }

    override fun onDestroy() {
        // The picker result can never arrive once this instance is gone, and a pending callback keeps
        // the WebView (and through it this Activity) reachable. Answering with null aborts the page's
        // outstanding <input type=file> request exactly as the user cancelling the picker would.
        runCatching { filePathCallback?.onReceiveValue(null) }
        filePathCallback = null
        super.onDestroy()
    }
}
