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

package com.github.yumeyucca.yumebox.runtime.service.session

import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.core.model.RunMode

data class RuntimeSpec(
    val profileUuid: String,
    val profileName: String,
    val profileDir: String,
    val ageSecretKey: String?,
    val overrideSpecs: List<OverrideSpec>,
    val runMode: RunMode,
    val skipRuntimePatches: Boolean,
    /** Compiles an inspect-only core configuration; never expose this as a user run mode. */
    val preview: Boolean,
)

/** A spec together with its one compile result; everything downstream reuses this YAML. */
class LoadedRuntime(
    val spec: RuntimeSpec,
    val config: String,
)
