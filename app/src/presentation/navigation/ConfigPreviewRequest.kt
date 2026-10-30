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

package com.github.yumeyucca.yumebox.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.yumeyucca.yumebox.core.model.LanguageScope

/**
 * One pending config preview: what to show and how to write edits back.
 *
 * The text cannot live in [Route.OverrideConfigPreview] — a config file is easily hundreds of
 * kilobytes and would sit in the navigation stack for as long as the route lives — so the request
 * travels beside the route as one immutable value. One value rather than four loose fields is what
 * makes the handoff atomic: a preview can never observe a new title with the previous body.
 */
@Immutable
data class ConfigPreviewRequest(
    val title: String,
    val content: String,
    val language: LanguageScope,
    /** Null for read-only previews (built-in overrides); the editor then hides its save action. */
    val onSave: (suspend (String) -> Unit)?,
)

/**
 * The slot a screen puts its [ConfigPreviewRequest] in before pushing
 * [Route.OverrideConfigPreview], and the preview route reads it back out.
 *
 * Data travels this way instead of as route arguments because neither half fits a route: the save
 * callback closes over the caller's own view model, and the document itself is the whole config file.
 * Routes here are never serialised either (every stack is created with `serializer = null`), so they
 * are not a place to park state that has to survive.
 *
 * Every [put] hands back a [previewId] the caller pushes with the route, so the route names exactly
 * one handoff instead of "whatever is in the slot right now". Opening the editor twice used to push
 * one identical route both times, which made the second push indistinguishable from the first and let
 * it be dropped: the editor then refused to open again until its page was left and re-entered.
 */
@Stable
class ConfigPreviewSlot {
    private var nextPreviewId = 0L
    private val requests = mutableStateMapOf<Long, ConfigPreviewRequest>()

    /** Stores [request] and returns the id to push with [Route.OverrideConfigPreview]. */
    fun put(request: ConfigPreviewRequest): Long {
        val previewId = nextPreviewId++
        requests[previewId] = request
        return previewId
    }

    /** One specific handoff, or null once its preview has been released. */
    fun get(previewId: Long): ConfigPreviewRequest? = requests[previewId]

    /**
     * Drops one handoff once its preview leaves the back stack, so the closure (and the config text
     * it captured) does not outlive the screen that showed it.
     *
     * Released by id rather than wholesale: a preview can still be on screen while a newer one is
     * already open, and the newer request must not be cleared under it.
     */
    fun release(previewId: Long) {
        requests.remove(previewId)
    }
}

/**
 * The [ConfigPreviewSlot] of the shell that hosts the route stack. A shell owns its own slot, so two
 * hosts (a root window and a sheet) can preview different files at the same time.
 */
val LocalConfigPreviewSlot = staticCompositionLocalOf<ConfigPreviewSlot> {
    error("LocalConfigPreviewSlot is not provided; host the config preview route inside a shell")
}
