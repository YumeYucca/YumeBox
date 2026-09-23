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

plugins {
    id("com.android.library")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.github.yumeyucca.yumebox.feature.substore"

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        // Single source of truth for the Javet native this APK can load. The runtime download URL
        // and the digest check are both derived from it, so the in-APK Java layer and the
        // out-of-APK native can never be asked to disagree.
        buildConfigField("String", "JAVET_VERSION", "\"${libs.versions.javetNodeAndroid.get()}\"")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":locale"))
    implementation(project(":ui"))
    implementation(project(":data"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.javet.node.android)
    implementation(libs.timber)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.preference)
}