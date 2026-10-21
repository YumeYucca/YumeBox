plugins {
    // Pinned instead of the version bundled with Gradle (6.7.6) because that one
    // resolves kotlin-gradle-plugin 2.4.10, which is affected by CVE-2026-53914.
    id("org.gradle.kotlin.kotlin-dsl") version "6.7.13"
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    //noinspection AndroidLintUseTomlInstead
    implementation("org.tukaani:xz:1.12")
}
