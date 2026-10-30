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

package com.github.yumeyucca.yumebox.data.store

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.github.yumeyucca.yumebox.data.model.RemoteProtocol
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Persistence boundary that keeps controller secrets out of MMKV plaintext. */
internal class RemoteBackendStorageCodec {
    fun decode(
        json: Json,
        encoded: String,
    ): List<RemoteBackend> =
        json.decodeFromString<List<StoredRemoteBackend>>(encoded).map { stored ->
            RemoteBackend(
                id = stored.id,
                name = stored.name,
                host = stored.host,
                port = stored.port,
                protocol = stored.protocol,
                secret =
                    when {
                        stored.secretCiphertext.isNotBlank() -> {
                            runCatching { RemoteBackendSecretCipher.decrypt(stored.secretCiphertext) }
                                .onFailure { error ->
                                    Timber.e(
                                        error,
                                        "Failed to decrypt remote controller secret for %s",
                                        stored.id,
                                    )
                                }.getOrDefault("")
                        }

                        else -> {
                            stored.secret.orEmpty()
                        }
                    },
            )
        }

    fun encode(
        json: Json,
        backends: List<RemoteBackend>,
    ): String =
        json.encodeToString(
            backends.map { backend ->
                StoredRemoteBackend(
                    id = backend.id,
                    name = backend.name,
                    host = backend.host,
                    port = backend.port,
                    protocol = backend.protocol,
                    secretCiphertext =
                        backend.secret
                            .takeIf(String::isNotBlank)
                            ?.let(RemoteBackendSecretCipher::encrypt)
                            .orEmpty(),
                )
            },
        )

    @Serializable
    private data class StoredRemoteBackend(
        val id: String,
        val name: String,
        val host: String,
        val port: Int,
        val protocol: RemoteProtocol = RemoteProtocol.HTTP,
        // Legacy field: decoded once and omitted from every new write.
        val secret: String? = null,
        val secretCiphertext: String = "",
    )
}

internal object RemoteBackendSecretCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "yumebox.remote-controller.secret.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val PAYLOAD_VERSION: Byte = 1
    private val keyLock = Any()

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val payload = byteArrayOf(PAYLOAD_VERSION) + cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        val payload = Base64.decode(ciphertext, Base64.NO_WRAP)
        require(payload.size > 1 + IV_BYTES) { "Remote controller secret payload is truncated" }
        require(payload[0] == PAYLOAD_VERSION) { "Unsupported remote controller secret version" }
        val iv = payload.copyOfRange(1, 1 + IV_BYTES)
        val encrypted = payload.copyOfRange(1 + IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey =
        synchronized(keyLock) {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateKey(keyStore)
        }

    private fun generateKey(keyStore: KeyStore): SecretKey {
        runCatching {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
                init(
                    KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        }.getOrElse { generationError ->
            // Another process may have created the alias between getKey and generateKey.
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: throw generationError
        }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
            ?: error("Android Keystore did not retain $KEY_ALIAS")
    }
}
