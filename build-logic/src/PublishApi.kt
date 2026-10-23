package dev.yume.build

import java.io.File
import java.nio.file.Files
import java.util.Properties

/** Native Toolchain publishing; the short-lived model override is restored under the packaging lock. */
fun publishApi(root: File, options: Map<String, String>, remote: Boolean) {
    val version = requireNotNull(options["api.version"]) { "Pass -Papi.version=<Maven version>" }
    require(Regex("[A-Za-z0-9_.+-]+").matches(version) && !version.contains("..")) { "Invalid Maven version" }
    val group = options["api.group"] ?: "io.github.yumeyucca.yumebox"
    require(Regex("[A-Za-z0-9_.-]+").matches(group) && !group.contains(".."))
    val repository = options["api.githubRepository"] ?: "YumeYucca/YumeBox"
    require(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+").matches(repository) && !repository.contains(".."))
    val module = root.resolve("api/module.yaml")
    val original = module.readText()
    val versionLine = Regex("(?m)^    version:.*$")
    require(versionLine.findAll(original).count() == 1) { "API publishing model changed" }
    val state = root.resolve(".kotlin/yumebox").apply { mkdirs() }
    val credentials = if (remote) Files.createTempFile(state.toPath(), "publishing-", ".properties").toFile() else null
    try {
        if (credentials != null) {
            val values = Properties().apply {
                setProperty("username", requireNotNull(System.getenv("GITHUB_ACTOR")) { "GITHUB_ACTOR is required" })
                setProperty("password", requireNotNull(System.getenv("GITHUB_TOKEN")) { "GITHUB_TOKEN is required" })
            }
            credentials.writer().use { values.store(it, null) }
        }
        val staging = root.resolve("build/api-publication").apply { mkdirs() }
        var configured = versionLine.replace(original, "    version: $version")
            .replace("    group: io.github.yumeyucca.yumebox", "    group: $group")
            .replace("https://maven.pkg.github.com/YumeYucca/YumeBox", "https://maven.pkg.github.com/$repository")
        if (credentials != null) configured = configured.replace("../.kotlin/yumebox/publishing.properties", "../.kotlin/yumebox/${credentials.name}")
        module.writeText(configured)
        try {
            val environment = if (remote) emptyMap() else mapOf("KOTLIN_CLI_JAVA_OPTIONS" to
                (System.getenv("KOTLIN_CLI_JAVA_OPTIONS").orEmpty() + " \"-Dmaven.repo.local=${staging.invariantSeparatorsPath}\"").trim())
            toolchain(root, listOf("publish", "--module=api", "--build-dir=${root.resolve("build/api-publish-build").path}", if (remote) "GitHubPackages" else "localStaging"), environment)
            if (!remote && !version.endsWith("-SNAPSHOT")) {
                val directory = staging.resolve("${group.replace('.', '/')}/api/$version")
                listOf(".jar", "-sources.jar", "-javadoc.jar", ".pom").forEach { suffix ->
                    check(directory.resolve("api-$version$suffix").isFile) { "API publication missing $suffix" }
                }
            }
        } finally {
            check(module.readText() == configured) { "API module changed during publishing; refusing to overwrite concurrent edits" }
            module.writeText(original)
        }
    } finally {
        if (credentials != null) check(credentials.delete()) { "Could not remove temporary publishing credentials" }
    }
}
