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

package com.github.yumeyucca.yumebox.screen.settings


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.core.bridge.Compiler
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton

/**
 * Age key generator dialog. The secret/public fields are editable so a key can be typed or pasted,
 * then "Derive Public Key" derives the public key from the entered secret, or "Generate" creates a
 * fresh key pair (x25519 or post-quantum hybrid) through the native JNI surface. Generated/entered
 * keys are usable end to end — the Rust override decryptor supports both x25519 and mlkem768x25519
 * hybrid identities at runtime.
 */
@Composable
fun AgeKeyGeneratorDialog(
    show: Boolean,
    hybrid: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing

    var secretKey by remember(show, hybrid) { mutableStateOf("") }
    var publicKey by remember(show, hybrid) { mutableStateOf("") }
    var generating by remember(show, hybrid) { mutableStateOf(false) }

    AppDialog(
        show = show,
        title =
            if (hybrid) YumeTxt.MetaFeature.AgeKey.HybridTitle
            else YumeTxt.MetaFeature.AgeKey.X25519Title,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            OemTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                label = YumeTxt.MetaFeature.AgeKey.SecretKey,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OemTextField(
                value = publicKey,
                onValueChange = { publicKey = it },
                label = YumeTxt.MetaFeature.AgeKey.PublicKey,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                TextButton(
                    text = YumeTxt.MetaFeature.AgeKey.DerivePublicKey,
                    onClick = {
                        scope.launch {
                            val derived =
                                withContext(Dispatchers.Default) {
                                    Compiler.nativeAgePublicKey(secretKey).takeIf {
                                        it.isNotBlank()
                                    }
                                }
                            if (!derived.isNullOrBlank()) {
                                publicKey = derived
                            }
                        }
                    },
                    enabled = secretKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = YumeTxt.MetaFeature.AgeKey.Generate,
                    onClick = {
                        if (generating) return@TextButton
                        generating = true
                        scope.launch {
                            val keyPair =
                                withContext(Dispatchers.Default) {
                                    // age x25519 keygen via liboverride (hybrid PQ keys are not
                                    // supported by the compiler crate; falls back to x25519).
                                    runCatching {
                                        kotlinx.serialization.json.Json.decodeFromString(
                                            com.github.yumeyucca.yumebox.core.model.AgeKeyPair
                                                .serializer(),
                                            Compiler.nativeGenAgeKey(),
                                        )
                                    }
                                        .getOrNull()
                                }
                            generating = false
                            if (keyPair != null) {
                                secretKey = keyPair.secretKey
                                publicKey = keyPair.publicKey
                            }
                        }
                    },
                    enabled = !generating,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
