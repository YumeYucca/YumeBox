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

package com.github.yumeyucca.yumebox.screen.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.presentation.component.AppFormDialog
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.component.PreferenceValueItem
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

private val ruleTypePresets =
    listOf(
        "DOMAIN",
        "DOMAIN-SUFFIX",
        "DOMAIN-KEYWORD",
        "DOMAIN-WILDCARD",
        "DOMAIN-REGEX",
        "GEOSITE",
        "IP-CIDR",
        "IP-CIDR6",
        "IP-SUFFIX",
        "IP-ASN",
        "GEOIP",
        "SRC-GEOIP",
        "SRC-IP-ASN",
        "SRC-IP-CIDR",
        "SRC-IP-SUFFIX",
        "DST-PORT",
        "SRC-PORT",
        "IN-PORT",
        "IN-TYPE",
        "IN-USER",
        "IN-NAME",
        "PROCESS-PATH",
        "PROCESS-PATH-WILDCARD",
        "PROCESS-PATH-REGEX",
        "PROCESS-NAME",
        "PROCESS-NAME-WILDCARD",
        "PROCESS-NAME-REGEX",
        "UID",
        "NETWORK",
        "DSCP",
        "RULE-SET",
        "AND",
        "OR",
        "NOT",
        "SUB-RULE",
        "MATCH",
    )

private val ruleExtraSupportedTypes = setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")

private fun supportsRuleExtra(ruleType: String): Boolean =
    ruleType.uppercase() in ruleExtraSupportedTypes

@Composable
internal fun SimpleTextEditorDialog(
    title: String,
    placeholder: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textFieldValue by
    remember(initialValue) {
        mutableStateOf(
            TextFieldValue(text = initialValue, selection = TextRange(initialValue.length))
        )
    }
    AppTextFieldDialog(
        show = true,
        title = title,
        textFieldValue = textFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            textFieldValue = updatedTextFieldValue
        },
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedValue = textFieldValue.text.trim()
            if (normalizedValue.isNotBlank()) {
                onConfirm(normalizedValue)
            }
        },
        label = placeholder,
    )
}

@Composable
internal fun KeyValueFormDialog(
    title: String,
    keyPlaceholder: String,
    valuePlaceholder: String,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    initialKey: String,
    initialValue: String,
    currentEditingKey: String? = null,
) {
    var keyTextFieldValue by
    remember(initialKey) {
        mutableStateOf(
            TextFieldValue(text = initialKey, selection = TextRange(initialKey.length))
        )
    }
    var valueTextFieldValue by
    remember(initialValue) {
        mutableStateOf(
            TextFieldValue(text = initialValue, selection = TextRange(initialValue.length))
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AppFormDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedKey = keyTextFieldValue.text.trim()
            val normalizedValue = valueTextFieldValue.text.trim()
            error =
                when {
                    normalizedKey.isBlank() -> YumeTxt.Component.Editor.Error.KeyEmpty
                    normalizedKey != currentEditingKey && normalizedKey in existingKeys ->
                        YumeTxt.Component.Editor.Error.KeyExists

                    else -> null
                }
            if (error == null) {
                onConfirm(normalizedKey, normalizedValue)
            }
        },
        error = error,
    ) {
        OemTextField(
            value = keyTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                keyTextFieldValue = updatedTextFieldValue
                error = null
            },
            label = keyPlaceholder,
            modifier = Modifier.fillMaxWidth(),
        )
        OemTextField(
            value = valueTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                valueTextFieldValue = updatedTextFieldValue
            },
            label = valuePlaceholder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun RuleEditorDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var ruleType by remember { mutableStateOf("DOMAIN-SUFFIX") }
    var payloadTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var target by remember { mutableStateOf(YumeTxt.Component.Editor.Rule.TargetReject) }
    var useSrc by remember { mutableStateOf(false) }
    var useNoResolve by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val targetItems = remember {
        listOf(
            YumeTxt.Component.Editor.Rule.TargetReject,
            YumeTxt.Component.Editor.Rule.TargetDirect,
            YumeTxt.Component.Editor.Rule.TargetMatch,
        )
    }
    val selectedRuleTypeIndex =
        remember(ruleType) {
            ruleTypePresets.indexOfFirst { it.equals(ruleType, ignoreCase = true) }.coerceAtLeast(0)
        }
    val selectedTargetIndex = remember(target) { targetItems.indexOf(target).coerceAtLeast(0) }

    AppFormDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = {
            val normalizedType = ruleType.trim().uppercase()
            val normalizedPayload = payloadTextFieldValue.text.trim()

            if (
                target != YumeTxt.Component.Editor.Rule.TargetMatch && normalizedPayload.isBlank()
            ) {
                error = YumeTxt.Component.Editor.Rule.ErrorContentRequired
                return@AppFormDialog
            }

            val result =
                if (target == YumeTxt.Component.Editor.Rule.TargetMatch) {
                    "MATCH"
                } else {
                    buildList {
                        add(normalizedType)
                        add(normalizedPayload)
                        add(target)
                        if (supportsRuleExtra(normalizedType)) {
                            if (useSrc) add("src")
                            if (useNoResolve) add("no-resolve")
                        }
                    }
                        .joinToString(",")
                }
            onConfirm(result)
        },
        error = error,
    ) {
        WindowDropdownPreference(
            title = YumeTxt.Component.Editor.Rule.Type,
            items = ruleTypePresets,
            selectedIndex = selectedRuleTypeIndex,
            onSelectedIndexChange = { index ->
                ruleType = ruleTypePresets.getOrElse(index) { ruleType }
                error = null
            },
        )
        WindowDropdownPreference(
            title = YumeTxt.Component.Editor.Rule.Target,
            items = targetItems,
            selectedIndex = selectedTargetIndex,
            onSelectedIndexChange = { index ->
                target = targetItems.getOrElse(index) { target }
                error = null
            },
        )
        OemTextField(
            value = payloadTextFieldValue,
            onValueChange = { updatedTextFieldValue ->
                payloadTextFieldValue = updatedTextFieldValue
                error = null
            },
            label = YumeTxt.Component.Editor.Rule.Content,
            modifier = Modifier.fillMaxWidth(),
        )
        if (supportsRuleExtra(ruleType)) {
            PreferenceValueItem(
                title = YumeTxt.Component.Editor.Rule.Src,
                summary = null,
                onClick = { useSrc = !useSrc },
                endActions = {
                    Checkbox(
                        state = if (useSrc) ToggleableState.On else ToggleableState.Off,
                        onClick = { useSrc = !useSrc },
                    )
                },
            )
            PreferenceValueItem(
                title = YumeTxt.Component.Editor.Rule.NoResolve,
                summary = null,
                onClick = { useNoResolve = !useNoResolve },
                endActions = {
                    Switch(
                        checked = useNoResolve,
                        onCheckedChange = { checked -> useNoResolve = checked },
                    )
                },
            )
        }
    }
}
