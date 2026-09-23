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

import com.android.build.gradle.tasks.PackageAndroidArtifact
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync

plugins {
    id("com.android.application")
}

dependencies {
    implementation(libs.javet.node.android)
}

android {
    namespace = gropify.project.namespace.extension

    defaultConfig {
        applicationId = gropify.project.namespace.extension
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.apply {
                clear()
                add("jniLibs")
            }
        }
    }

    tasks.withType<PackageAndroidArtifact>().configureEach {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf("META-INF/**")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            val abiList =
                (gropify.abi.extension.list ?: "arm64-v8a")
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            // Qualify receiver so Kotlin does not resolve include() against Iterable from the
            // chain.
            @Suppress("SpreadOperator") this@abi.include(*abiList.toTypedArray())
            isUniversalApk = false
        }
    }
}

tasks.register<Sync>("collectJavetNative") {
    group = "distribution"
    description = "Extracts the arm64-v8a Javet native library for the Expand release"
    from(provider { configurations.getByName("releaseRuntimeClasspath").files.map(::zipTree) }) {
        // Derived from the catalog so the packaged native matches the runtime URL in
        // feature/substore; a new version also needs its digests in NativeLibraryManager.
        include("jni/arm64-v8a/libjavet-node-android.v.${libs.versions.javetNodeAndroid.get()}.so")
        rename { "libjavet.so" }
        eachFile { relativePath = RelativePath(true, name) }
        includeEmptyDirs = false
    }
    into(rootProject.layout.projectDirectory.dir("output_apk/Expand"))
}

tasks.register<Exec>("compressJavetNative") {
    group = "distribution"
    description = "Compresses the Javet native library as an XZ release asset"
    dependsOn("collectJavetNative")

    val inputFile = rootProject.layout.projectDirectory.file("output_apk/Expand/libjavet.so")
    val outputFile = rootProject.layout.projectDirectory.file("output_apk/Expand/libjavet.so.xz")
    inputs.file(inputFile)
    outputs.file(outputFile)
    commandLine("7z", "a", "-y", "-txz", "-mx=9", outputFile.asFile, inputFile.asFile)
}
