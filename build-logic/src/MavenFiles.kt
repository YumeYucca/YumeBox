package dev.yume.build

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class MavenFiles(root: File) {
    private val local = root.resolve(".kotlin/yumebox/maven")
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build()
    private val shared = when {
        System.getProperty("os.name").startsWith("Windows") -> File(System.getenv("LOCALAPPDATA"), "JetBrains/Kotlin/.m2.cache")
        System.getProperty("os.name").startsWith("Mac") -> File(System.getProperty("user.home"), "Library/Caches/JetBrains/Kotlin/.m2.cache")
        else -> File(System.getenv("XDG_CACHE_HOME") ?: File(System.getProperty("user.home"), ".cache").path, "JetBrains/Kotlin/.m2.cache")
    }
    private val repositories = listOf("https://repo1.maven.org/maven2", "https://dl.google.com/dl/android/maven2", "https://jitpack.io", "https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")

    fun artifact(group: String, name: String, version: String, extension: String): File =
        get("${group.replace('.', '/')}/$name/$version/$name-$version.$extension")

    fun get(path: String): File {
        require(!path.startsWith('/') && path.split('/').none { it == ".." })
        shared.resolve(path).takeIf { it.isFile }?.let { return it }
        val destination = local.resolve(path)
        if (destination.isFile) return destination
        for (repository in repositories) {
            val response = runCatching {
                client.send(HttpRequest.newBuilder(URI.create("$repository/$path")).timeout(Duration.ofSeconds(45)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
            }.getOrNull() ?: continue
            if (response.statusCode() != 200) continue
            destination.parentFile.mkdirs()
            val temporary = File.createTempFile("download-", ".tmp", destination.parentFile)
            temporary.writeBytes(response.body())
            java.nio.file.Files.move(temporary.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            return destination
        }
        error("Required Maven file unavailable: $path")
    }
}
