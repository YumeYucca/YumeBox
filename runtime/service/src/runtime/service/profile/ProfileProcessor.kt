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

package com.github.yumeyucca.yumebox.runtime.service.profile

import android.content.Context
import com.github.yumeyucca.yumebox.runtime.api.FetchObserver
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.ProfileUpdateReport
import com.github.yumeyucca.yumebox.runtime.api.ProviderPrefetchReport
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.util.importedDir
import com.github.yumeyucca.yumebox.runtime.service.util.sendProfileChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

/**
 * Orchestrates profile update/delete: locks, staging commit/rollback, subscription fetch, and best-effort external
 * provider prefetch. Heavy lifting lives in focused modules under this package.
 */
object ProfileProcessor {
    private val profileLock = Mutex()
    private val processLock = Mutex()

    private data class UpdateSnapshot(
        val imported: Imported,
        val hasCommittedConfig: Boolean,
    )

    @Suppress("TooGenericExceptionCaught")
    suspend fun update(
        context: Context,
        uuid: UUID,
        callback: FetchObserver?,
    ): ProfileUpdateReport {
        return withContext(Dispatchers.IO + NonCancellable) {
            processLock.withLock {
                val targetDir = context.importedDir.resolve(uuid.toString())
                val stagingDir = context.cacheDir.resolve("profile-staging").resolve(uuid.toString())
                val snapshot =
                    profileLock.withLock {
                        val imported =
                            ImportedDao.queryByUUID(uuid) ?: throw IllegalArgumentException("profile $uuid not found")

                        stagingDir.deleteRecursively()
                        stagingDir.mkdirs()

                        if (targetDir.exists()) {
                            targetDir.copyRecursively(stagingDir, overwrite = true)
                        }

                        UpdateSnapshot(
                            imported = imported,
                            // Sentinel "this profile already committed a config", used below to
                            // decide whether a failed update may roll back (delete) it.
                            hasCommittedConfig = targetDir.resolve("config.yaml").isFile,
                        )
                    }

                var cb = callback
                var subInfo: SubscriptionInfo? = null
                var providerReport = ProviderPrefetchReport()

                val isFirstImport = !snapshot.hasCommittedConfig

                try {
                    // Only Url profiles are fetched: a File profile's config.yaml was already
                    // written at import time, so HTTP-getting its local source would just clobber it.
                    if (snapshot.imported.type == Profile.Type.Url) {
                        fetchSubscription(stagingDir, snapshot.imported.source) { status ->
                            val fetchedSubInfo = status.toSubscriptionInfo()
                            if (fetchedSubInfo != null) {
                                subInfo = fetchedSubInfo
                                return@fetchSubscription
                            }
                            try {
                                cb?.updateStatus(status)
                            } catch (error: Exception) {
                                // fault barrier: the observer may live across a binder; reporting
                                // failures must not abort the profile fetch itself.
                                cb = null
                                Timber.w(error, "Report fetch status: %s", error.message)
                            }
                        }
                    } else {
                        // Local/file profiles still need provider prefetch. Re-materialize the
                        // original local source into staging so a missing copy cannot skip every
                        // proxy/rule provider download.
                        materializeLocalConfig(context, stagingDir, snapshot.imported)
                    }

                    // First-time import only: reject broken main configs before provider downloads
                    // or commit. Updates keep the previous committed profile if the new payload is
                    // invalid at runtime instead of blocking refresh here.
                    if (isFirstImport) {
                        val stagedMain = stagingDir.resolve("config.yaml")
                        check(stagedMain.isFile && stagedMain.length() > 0L) {
                            "Profile import produced no config.yaml: ${snapshot.imported.uuid}"
                        }
                        // Throws on a failed core test, which the catch below turns into a full
                        // rollback: a subscription the core rejects is never committed.
                        ProfileConfigTester.validateMainConfigOrThrow(context, stagedMain)
                    }

                    providerReport =
                        fetchExternalProviders(
                            context = context,
                            uuid = snapshot.imported.uuid,
                            stagingDir = stagingDir,
                            profileDir = targetDir,
                            ageSecretKey = snapshot.imported.ageSecretKey,
                        ) { status ->
                            try {
                                cb?.updateStatus(status)
                            } catch (error: Exception) {
                                cb = null
                                Timber.w(error, "Report provider fetch status: %s", error.message)
                            }
                        }
                    val stagedConfig = stagingDir.resolve("config.yaml")
                    check(stagedConfig.isFile && stagedConfig.length() > 0L) {
                        "Profile update produced no config.yaml: ${snapshot.imported.uuid}"
                    }

                    profileLock.withLock {
                        if (ImportedDao.exists(snapshot.imported.uuid)) {
                            targetDir.deleteRecursively()
                            stagingDir.copyRecursively(targetDir, overwrite = true)

                            val finalName =
                                if (snapshot.imported.type == Profile.Type.Url) {
                                    resolveSubscriptionName(
                                        snapshot.imported.name,
                                        snapshot.imported.source,
                                        subInfo,
                                    )
                                } else {
                                    snapshot.imported.name
                                }

                            val updated =
                                Imported(
                                    snapshot.imported.uuid,
                                    finalName,
                                    snapshot.imported.type,
                                    snapshot.imported.source,
                                    subInfo?.updateInterval ?: snapshot.imported.interval,
                                    subInfo?.upload ?: snapshot.imported.upload,
                                    subInfo?.download ?: snapshot.imported.download,
                                    subInfo?.total ?: snapshot.imported.total,
                                    subInfo?.expire ?: snapshot.imported.expire,
                                    snapshot.imported.createdAt,
                                    ageSecretKey = snapshot.imported.ageSecretKey,
                                )
                            ImportedDao.update(updated)

                            context.sendProfileChanged(
                                snapshot.imported.uuid,
                                affectsRuntime = ServiceStore().activeProfile == snapshot.imported.uuid,
                            )
                        }
                    }

                    ProfileUpdateReport(providers = providerReport)
                } catch (error: Exception) {
                    // fault barrier: roll back the staged update atomically, then rethrow.
                    profileLock.withLock {
                        if (!snapshot.hasCommittedConfig && ImportedDao.exists(snapshot.imported.uuid)) {
                            ImportedDao.remove(snapshot.imported.uuid)
                            targetDir.deleteRecursively()
                            context.sendProfileChanged(
                                snapshot.imported.uuid,
                                affectsRuntime = ServiceStore().activeProfile == snapshot.imported.uuid,
                            )
                        }
                    }
                    val errorMessage = error.message ?: ""
                    if (
                        errorMessage.contains("no identities specified") ||
                        errorMessage.contains("decrypt config error")
                    ) {
                        throw IllegalArgumentException(
                            "This config is encrypted with age. Please provide the age secret key when importing the config.",
                            error,
                        )
                    } else {
                        throw error
                    }
                } finally {
                    stagingDir.deleteRecursively()
                }
            }
        }
    }

    suspend fun delete(
        context: Context,
        uuid: UUID,
    ) {
        withContext(Dispatchers.IO + NonCancellable) {
            profileLock.withLock {
                val affectsRuntime = ServiceStore().activeProfile == uuid
                ImportedDao.remove(uuid)

                val imported = context.importedDir.resolve(uuid.toString())
                imported.deleteRecursively()

                context.sendProfileChanged(uuid, affectsRuntime = affectsRuntime)
            }
        }
    }
}
