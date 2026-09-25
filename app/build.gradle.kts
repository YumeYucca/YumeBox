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

@file:Suppress("UnstableApiUsage")

import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import dev.yume.packer.BuildLoaderDexTask
import dev.yume.packer.PackApkTask
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.android.application")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("com.mikepenz.aboutlibraries.plugin.android")
}

abstract class TransformPackedApksTask : PackApkTask() {
    @get:Internal
    abstract val transformationRequest:
        Property<ArtifactTransformationRequest<TransformPackedApksTask>>

    @TaskAction
    fun transform() {
        val outputRoot = outputApkDirectory.get().asFile
        outputRoot.deleteRecursively()
        outputRoot.mkdirs()
        transformationRequest.get().submit(this) { artifact ->
            val input = File(artifact.outputFile)
            val output = outputRoot.resolve(input.name)
            packApk(input, output)
            output
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val appAbiList = gropify.abi.app.list.split(',').map { it.trim() }.filter { it.isNotEmpty() }
check(appAbiList == listOf("arm64-v8a")) {
    "Only arm64-v8a is supported; configured ABIs: $appAbiList"
}

// Packaging switches. CLI -P properties (same pattern as build.number below), NOT gropify keys:
//  - geo.bundle=false   -> keep the XZ geo databases out of assets (the local
//    default); a fresh install must run a Builtin APK once before it can start the local core.
// Release-only native-lib XZ compression is handled by the dev.yume.packer APK transform (which
// keeps the libmihomo PIE shell and libloader raw in nativeLibraryDir and packs the shared Go core
// plus the remaining libraries into assets/loader/), not here.
val geoBundle = providers.gradleProperty("geo.bundle").orNull?.toBoolean() ?: false
val splitAbiList = listOf("arm64-v8a")
val geoFilesAssetsDir = rootProject.layout.buildDirectory.dir("generated/assets/geo")
val signingPropertiesFile = rootProject.file("signing.properties")
val releaseSigningProperties =
    signingPropertiesFile.takeIf(File::isFile)?.let { file ->
        Properties().apply { file.inputStream().use(::load) }
    }

// CI-computed build versioning. CI injects `-Pbuild.number=<N>` where N is the commit
// count of the built commit's parent chain (`git rev-list --count HEAD`, computed inside
// reusable-build-apk-only.yml / reusable-prepare-publish.yml after a fetch-depth:0
// checkout), plus `-Pbuild.hash=<commit sha>` and `-Pbuild.branch=<branch name>`; local
// builds fall back to the epoch alone. versionCode = epoch + N, where the epoch is
// `project.version.code` from gradle.properties (do not hardcode it here). Channel, PR and
// official release builds all share this one formula, so every published APK gets a unique,
// comparable versionCode (releases are no longer stuck at the bare epoch below channel
// packages). Monotonic vs the retired ci-channel run_number scheme: the commit count only
// grows along a branch and is >= the number of pushes >= run_number, so the new sequence
// never sorts below the already-published epoch+run_number packages; if history is ever
// rewritten so the count shrinks, bump project.version.code above the last published
// versionCode instead. versionName = <base>[.<branch>].<hash8> only when a hash is
// injected - official releases pass no hash/branch, so they keep the clean base version
// (e.g. 0.5.2) while still getting a unique versionCode.
val baseVersionCode = gropify.project.version.code
val ciBuildNumber =
    providers.gradleProperty("build.number").orNull?.trim()?.takeIf { it.isNotEmpty() }?.toInt()
val ciBuildHash =
    providers.gradleProperty("build.hash").orNull?.trim()?.takeIf { it.isNotEmpty() }?.take(8)
// Branch segment normalization (must stay in sync with reusable-prepare-publish.yml):
// lowercase, every non-[a-z0-9] run collapses to a single '-', leading/trailing '-' trimmed.
val ciBuildBranch =
    providers
        .gradleProperty("build.branch")
        .orNull
        ?.lowercase()
        ?.replace(Regex("[^a-z0-9]+"), "-")
        ?.trim('-')
        ?.takeIf { it.isNotEmpty() }
val appVersionCode = baseVersionCode + (ciBuildNumber ?: 0)
val appVersionName =
    ciBuildHash?.let { hash ->
        listOfNotNull(gropify.project.version.name, ciBuildBranch, hash).joinToString(".")
    } ?: gropify.project.version.name

// Published APK file names are produced directly by Gradle. CI supplies the tail and
// optional channel segment once per workflow run; local builds omit both.
val apkOutputPrefix =
    providers.gradleProperty("apk.output.prefix").orNull?.trim()?.takeIf { it.isNotEmpty() }
        ?: gropify.project.name
val apkOutputTail =
    providers.gradleProperty("apk.output.tail").orNull?.trim()?.takeIf { it.isNotEmpty() }
val apkChannelSegment =
    providers.gradleProperty("apk.output.channel").orNull?.trim()?.takeIf { it.isNotEmpty() }
val apkGeoSegment = if (geoBundle) "builtin" else "external"

// Resolve the tracked mihomo tree at configure time so About / BuildConfig can show branch + hash
// without a runtime JNI probe (the core is out-of-process now). Prefer kernel.properties for the
// channel label (Alpha or Meta); fall back to the git checkout under
// external.mihomo.dir for the short commit.
// CI APK jobs never have the mihomo tree — they only download jniLibs — so prefer the
// core-version.properties stamp written by scripts/native-build.py during --go.
data class MihomoBuildInfo(
    val branch: String,
    val commit: String,
    val displayVersion: String,
    val gitVersionArg: String,
)

fun escapeBuildConfigString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

// File-only git lookup: configuration cache forbids starting external processes during
// configuration (ProcessBuilder/git rev-parse). Reading .git/HEAD + refs is enough for
// branch + short hash and stays cache-compatible.
fun resolveGitDir(repoDir: File): File? {
    val git = File(repoDir, ".git")
    when {
        git.isDirectory -> return git
        git.isFile -> {
            val content = git.readText().trim()
            if (!content.startsWith("gitdir:")) return null
            val path = content.removePrefix("gitdir:").trim()
            val resolved = File(path)
            return if (resolved.isAbsolute) resolved else File(repoDir, path)
        }
        else -> return null
    }
}

/** Resolve the effective object store for a (worktree) git dir via commondir when present. */
fun resolveGitCommonDir(gitDir: File): File {
    val commonFile = File(gitDir, "commondir")
    if (!commonFile.isFile) return gitDir
    val raw = commonFile.readText().trim()
    if (raw.isEmpty()) return gitDir
    val resolved = File(raw)
    val common = if (resolved.isAbsolute) resolved else File(gitDir, raw)
    return if (common.isDirectory) common else gitDir
}

fun readGitRef(gitDir: File, ref: String): String? {
    val candidates = listOf(gitDir, resolveGitCommonDir(gitDir)).distinctBy { it.canonicalPath }
    for (dir in candidates) {
        val refFile = File(dir, ref)
        if (refFile.isFile) {
            return refFile.readText().trim().takeIf { it.isNotEmpty() }
        }
        val packed = File(dir, "packed-refs")
        if (!packed.isFile) continue
        packed.useLines { lines ->
            for (line in lines) {
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("^")) continue
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2 && parts[1] == ref) {
                    return parts[0].trim().takeIf { it.isNotEmpty() }
                }
            }
        }
    }
    return null
}

/** @return shortCommit to optional branch name (null when detached). */
fun readGitHead(repoDir: File): Pair<String, String?> {
    val gitDir = resolveGitDir(repoDir) ?: return "unknown" to null
    val headFile = File(gitDir, "HEAD")
    if (!headFile.isFile) return "unknown" to null
    val head = headFile.readText().trim()
    if (head.startsWith("ref:")) {
        val ref = head.removePrefix("ref:").trim()
        val branch =
            ref.removePrefix("refs/heads/").takeIf {
                ref.startsWith("refs/heads/") && it.isNotEmpty()
            }
        // Prefer the local branch ref; some shallow/single-branch checkouts only pack the remote.
        val full =
            readGitRef(gitDir, ref)
                ?: branch?.let { readGitRef(gitDir, "refs/remotes/origin/$it") }
                ?: return "unknown" to branch
        return full.take(8) to branch
    }
    // Detached HEAD stores the raw commit object name.
    return head.take(8).ifEmpty { "unknown" } to null
}

fun loadCoreVersionStamp(rootDir: File): Properties? {
    val candidates = buildList {
        add(rootDir.resolve("build/generated/core-version.properties"))
        val jniRoot = rootDir.resolve("jniLibs")
        if (jniRoot.isDirectory) {
            jniRoot
                .listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name }
                ?.forEach { abiDir -> add(File(abiDir, "core-version.properties")) }
        }
    }
    for (file in candidates) {
        if (!file.isFile) continue
        val loaded = Properties()
        file.inputStream().use { loaded.load(it) }
        val commit = loaded.getProperty("core.commit")?.trim().orEmpty()
        if (commit.isNotEmpty() && commit != "unknown") {
            return loaded
        }
    }
    return null
}

fun resolveMihomoBuildInfo(rootDir: File): MihomoBuildInfo {
    val props = Properties()
    val kernelFile = rootDir.resolve("kernel.properties")
    if (kernelFile.isFile) {
        kernelFile.inputStream().use { props.load(it) }
    }
    val configuredBranch =
        props.getProperty("external.mihomo.branch", "Alpha").trim().ifEmpty { "Alpha" }
    val suffix = props.getProperty("external.mihomo.suffix", "").trim()
    val includeTimestamp =
        props.getProperty("external.mihomo.includeTimestamp", "false").toBooleanStrictOrNull()
            ?: false
    val mihomoRel = props.getProperty("external.mihomo.dir", "lib/mihomo/mihomo").trim()
    val mihomoDir = rootDir.resolve(mihomoRel)

    // Resolution order:
    // 1) -Pcore.branch / -Pcore.commit overrides
    // 2) live git checkout under external.mihomo.dir (local dev — freshest)
    // 3) core-version.properties stamped by the native Python build tool (CI APK job has no mihomo tree)
    val propBranch =
        providers.gradleProperty("core.branch").orNull?.trim()?.takeIf { it.isNotEmpty() }
    val propCommit =
        providers.gradleProperty("core.commit").orNull?.trim()?.takeIf { it.isNotEmpty() }
    val versionStamp = loadCoreVersionStamp(rootDir)
    val (gitCommit, gitBranch) = readGitHead(mihomoDir)
    val liveGitCommit = gitCommit.takeIf { it != "unknown" }
    val stampCommit = versionStamp?.getProperty("core.commit")?.trim()?.takeIf { it.isNotEmpty() }
    val usingStampCommit = propCommit == null && liveGitCommit == null && stampCommit != null

    val commit = propCommit ?: liveGitCommit ?: stampCommit ?: "unknown"
    // Channel from kernel.properties is the product label; stamp / git branch are fallbacks.
    val branchBase = configuredBranch.ifBlank { gitBranch ?: "mihomo" }
    val branchLabel =
        propBranch
            ?: if (usingStampCommit) {
                versionStamp?.getProperty("core.branch")?.trim()?.takeIf { it.isNotEmpty() }
                    ?: (branchBase + suffix)
            } else {
                branchBase + suffix
            }
    val timeStamp =
        if (includeTimestamp) {
            // Avoid java.* package paths: in Gradle Kotlin DSL `java` is the project extension.
            SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(Date())
        } else {
            "local"
        }
    // Historical version.h style: Alpha-06249f84
    // Only reuse stamp display/gitVersion when the commit itself came from the stamp (CI path).
    val display =
        if (usingStampCommit) {
            versionStamp?.getProperty("core.displayVersion")?.trim()?.takeIf { it.isNotEmpty() }
                ?: "$branchLabel-$commit"
        } else {
            "$branchLabel-$commit"
        }
    // Go --git-version flag shape: BRANCH_HASH_TIME (see native/delegate/init.go).
    val gitVersionArg =
        if (usingStampCommit) {
            versionStamp?.getProperty("core.gitVersion")?.trim()?.takeIf { it.isNotEmpty() }
                ?: "${branchLabel.replace('_', '-')}_${commit}_$timeStamp"
        } else {
            "${branchLabel.replace('_', '-')}_${commit}_$timeStamp"
        }
    return MihomoBuildInfo(
        branch = branchLabel,
        commit = commit,
        displayVersion = display,
        gitVersionArg = gitVersionArg,
    )
}

val mihomoBuildInfo = resolveMihomoBuildInfo(rootProject.projectDir)

android {
    namespace = gropify.project.namespace.base

    defaultConfig {
        applicationId = gropify.project.namespace.base
        targetSdk = gropify.android.targetSdk
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "BASE_VERSION", "\"${gropify.project.version.name}\"")
        // Mihomo core identity (branch + short hash) from kernel.properties / lib/mihomo checkout.
        buildConfigField(
            "String",
            "CORE_BRANCH",
            "\"${escapeBuildConfigString(mihomoBuildInfo.branch)}\"",
        )
        buildConfigField(
            "String",
            "CORE_COMMIT",
            "\"${escapeBuildConfigString(mihomoBuildInfo.commit)}\"",
        )
        buildConfigField(
            "String",
            "CORE_VERSION",
            "\"${escapeBuildConfigString(mihomoBuildInfo.displayVersion)}\"",
        )
        buildConfigField(
            "String",
            "CORE_GIT_VERSION",
            "\"${escapeBuildConfigString(mihomoBuildInfo.gitVersionArg)}\"",
        )
        manifestPlaceholders["appName"] = gropify.project.name
        manifestPlaceholders["applicationClass"] = ".App"
        manifestPlaceholders["componentFactory"] = "androidx.core.app.CoreComponentFactory"
    }

    compileOptions {
        val javaVer = gropify.android.jvm
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.apply {
                clear()
                add("src")
            }
            res.directories.apply {
                clear()
                add("res")
            }
            assets.directories.apply {
                clear()
                add("assets")
                if (geoBundle) {
                    add(geoFilesAssetsDir.get().asFile.invariantSeparatorsPath)
                }
            }
            aidl.directories.apply {
                clear()
                add("aidl")
            }
            resources.directories.apply {
                clear()
                add("resources")
            }
            jniLibs.directories.apply {
                clear()
                add("../jniLibs")
            }
            if (project.file("AndroidManifest.xml").isFile) {
                manifest.srcFile("AndroidManifest.xml")
            }
        }
        getByName("test") {
            kotlin.directories.apply {
                clear()
                add("test")
            }
        }
    }

    androidResources {
        generateLocaleConfig = false
        ignoreAssetsPattern =
            "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~:!BundleMRS.7z"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            manifestPlaceholders["applicationClass"] = "dev.yume.loader.LoaderApplication"
            manifestPlaceholders["componentFactory"] = "dev.yume.loader.LoaderComponentFactory"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-loader.pro",
            )
        }
    }

    splits {
        abi {
            //noinspection WrongGradleMethod
            isEnable =
                gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            // AGP Split.include only accepts vararg; copying this tiny ABI list is negligible.
            @Suppress("SpreadOperator")
            //noinspection ChromeOsAbiSupport
            include(*splitAbiList.toTypedArray())
            isUniversalApk = false
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("lib/**/libjavet*.so")
            useLegacyPackaging = true
        }
        resources {
            excludes.add("META-INF/**")
            excludes.add("okhttp3/**")
            excludes.add("schema/**")
            excludes.add("assets/dexopt/**")
            excludes.add("tables/**")
            excludes.add("DebugProbesKt.bin")
            excludes.add("kotlin-tooling-metadata.json")
            excludes.add("**/*.kotlin_builtins")
            excludes.add("**/*.kotlin_module")
            excludes.add("**/*.properties")
            excludes.add("**/*.txt")
        }
    }

    signingConfigs {
        if (releaseSigningProperties != null) {
            create("release") {
                storeFile = rootProject.file("release.keystore")
                storePassword = releaseSigningProperties.getProperty("keystore.password")!!
                keyAlias = releaseSigningProperties.getProperty("key.alias")!!
                keyPassword = releaseSigningProperties.getProperty("key.password")!!
            }
        }
    }

    if (signingConfigs.findByName("release") != null) {
        buildTypes.named("release").configure {
            signingConfig = signingConfigs.getByName("release")
        }
        buildTypes.named("debug").configure {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    //noinspection WrongGradleMethod
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiName =
                    output.filters
                        .find {
                            it.filterType ==
                                com.android.build.api.variant.FilterConfiguration.FilterType.ABI
                        }
                        ?.identifier ?: "arm64-v8a"
                val outputName =
                    buildList {
                            add(apkOutputPrefix)
                            add(apkGeoSegment)
                            if (abiName != "arm64-v8a") add(abiName)
                            apkChannelSegment?.let(::add)
                            apkOutputTail?.let(::add)
                        }
                        .joinToString("-") + ".apk"
                output.versionName.set(appVersionName)
                (output as com.android.build.api.variant.impl.VariantOutputImpl)
                    .outputFileName
                    .set(outputName)
            }
        }
    }
}

val loaderRuntime =
    configurations.detachedConfiguration(
        dependencies.create("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    )
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val capitalized = variant.name.replaceFirstChar(Char::uppercaseChar)
        val loaderDexTask =
            tasks.register<BuildLoaderDexTask>("build${capitalized}LoaderDex") {
                group = "build"
                description = "Builds the standalone loader DEX for ${variant.name}"
                loaderAar.set(
                    project(":pack").layout.buildDirectory.file("outputs/aar/pack-release.aar")
                )
                runtimeArtifacts.from(loaderRuntime)
                sdkDirectory.set(sdkComponents.sdkDirectory)
                minSdk.set(variant.minSdk.apiLevel)
                outputDirectory.set(
                    layout.buildDirectory.dir(
                        "intermediates/yumePacker/${variant.name}/loaderDex"
                    )
                )
                dependsOn(":pack:bundleReleaseAar")
            }
        val packApkTask =
            tasks.register<TransformPackedApksTask>("pack${capitalized}Apk") {
                group = "build"
                description =
                    "Compresses DEX payloads and installs the loader in ${variant.name} APKs"
                loaderDex.set(loaderDexTask.flatMap { it.outputDirectory.file("classes.dex") })
                sdkDirectory.set(sdkComponents.sdkDirectory)
                originalApplication.set("com.github.yumeyucca.yumebox.App")
                originalComponentFactory.set("androidx.core.app.CoreComponentFactory")
                if (releaseSigningProperties != null) {
                    keyStoreFile.set(rootProject.layout.projectDirectory.file("release.keystore"))
                    keyStorePassword.set(
                        releaseSigningProperties.getProperty("keystore.password")
                    )
                    keyAlias.set(releaseSigningProperties.getProperty("key.alias"))
                    keyPassword.set(releaseSigningProperties.getProperty("key.password"))
                }
            }
        val artifactRequest =
            variant.artifacts
                .use(packApkTask)
                .wiredWithDirectories(
                    TransformPackedApksTask::inputApkDirectory,
                    TransformPackedApksTask::outputApkDirectory,
                )
                .toTransformMany(SingleArtifact.APK)
        packApkTask.configure { transformationRequest.set(artifactRequest) }
    }
}

listOf("debug", "release").forEach { buildType ->
    val capitalized = buildType.replaceFirstChar(Char::uppercaseChar)
    val collectApkTask =
        tasks.register<Sync>("collect${capitalized}Apk") {
            group = "build"
            description = "Copies ${buildType} APKs to the root output_apk directory"
            from(layout.buildDirectory.dir("outputs/apk/$buildType")) { include("*.apk") }
            into(rootProject.layout.projectDirectory.dir("output_apk/$buildType"))
        }
    tasks.matching { it.name == "assemble$capitalized" }.configureEach {
        finalizedBy(collectApkTask)
    }
}

dependencies {
    implementation(libs.androidx.animation)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":runtime:api"))
    implementation(project(":runtime:client"))
    implementation(project(":runtime:service"))
    implementation(project(":feature:substore"))
    implementation(project(":feature:proxy"))
    implementation(project(":feature:override"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:meta"))
    compileOnly(project(":pack"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.icons)
    implementation(libs.haze)
    implementation(libs.haze.blur)
    implementation(libs.haze.blur.materials)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)

    val mmkvVersion = libs.versions.mmkv64.get()
    //noinspection NewerVersionAvailable
    //noinspection AndroidLintUseTomlInstead,AndroidLintNewerVersionAvailable
    implementation("com.tencent:mmkv:$mmkvVersion")

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.timber)
    implementation(libs.xz)
    implementation(libs.smali.dexlib2) {
        exclude(group = "com.google.guava", module = "guava")
    }

    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.video)

    implementation(libs.sketch.compose)
    implementation(libs.sketch.http)
    implementation(libs.sketch.animated.gif)
    implementation(libs.sketch.animated.heif)
    implementation(libs.sketch.animated.webp)
    implementation(libs.sketch.animated.gif.koral)

    implementation(libs.reorderable)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hiddenapibypass)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    testImplementation("junit:junit:4.13.2")
}
