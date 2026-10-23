package dev.yume.build

import dev.yume.packer.*
import java.io.File
import java.io.StringWriter
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID
import kotlinx.serialization.json.*

fun packageApplication(arguments: Array<String>) {
    if (arguments.any { it == "--help" }) {
        println("kotlin run -m packaging -- --module=app --build-type=release [-Pkey=value] [--compile-only] [--allow-unsigned]")
        return
    }
    val options = linkedMapOf<String, String>()
    val flags = mutableSetOf<String>()
    arguments.forEach { argument ->
        when {
            argument.startsWith("-P") && '=' in argument -> options[argument.substring(2).substringBefore('=')] = argument.substringAfter('=')
            argument.startsWith("--") && '=' in argument -> options[argument.substring(2).substringBefore('=')] = argument.substringAfter('=')
            argument in setOf("--compile-only", "--allow-unsigned", "--prepare-api", "--publish-api") -> flags += argument
            else -> error("Unknown packaging argument: $argument")
        }
    }
    val accepted = setOf("module", "build-type", "geo.bundle", "build.number", "build.hash", "build.branch", "apk.output.prefix", "apk.output.tail", "apk.output.channel", "core.branch", "core.commit", "update.channel", "update.tag", "update.metaAssetName", "api.version", "api.group", "api.githubRepository")
    require(options.keys.all { it in accepted }) { "Unsupported packaging options: ${options.keys - accepted}" }
    val root = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
        .firstOrNull { it.resolve("product.properties").isFile && it.resolve("project.yaml").isFile }
        ?: error("Run packaging from the YumeBox checkout")
    val state = root.resolve(".kotlin/yumebox").apply { mkdirs() }
    FileChannel.open(state.resolve("packaging.lock").toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        channel.lock().use { packageLocked(root, state, options, flags) }
    }
}

private fun packageLocked(root: File, state: File, options: Map<String, String>, flags: Set<String>) {
    if ("--prepare-api" in flags || "--publish-api" in flags) {
        require(!flags.containsAll(setOf("--prepare-api", "--publish-api")))
        publishApi(root, options, "--publish-api" in flags)
        return
    }
    val module = options["module"] ?: "app"
    val variant = options["build-type"] ?: "debug"
    require(module == "app" && variant in setOf("debug", "release")) { "Unsupported module or build type" }
    val request = loadProperties(root.resolve("product.properties")).apply {
        options.filterKeys { it != "module" && it != "build-type" }.forEach { (key, value) -> setProperty(key, value) }
        setProperty("build.type", variant)
    }
    resolveCoreIdentity(root, request).forEach { (key, value) -> request.setProperty(key, value) }
    val plan = requestPlan(request)
    val requestFile = state.resolve("request.properties")
    val serialized = StringWriter().also { request.store(it, null) }.toString().lineSequence()
        .filter { it.isNotEmpty() && !it.startsWith('#') }.sorted().joinToString(System.lineSeparator(), postfix = System.lineSeparator())
    if (!requestFile.isFile || requestFile.readText() != serialized) requestFile.writeText(serialized)
    val maven = MavenFiles(root)
    println("Packaging $module $variant: ${plan.versionName} (${plan.versionCode}), geo=${plan.geoSegment}")
    val graphText = toolchain(root, listOf("--log-level=error", "show", "dependencies", "--module=$module", "--platform-group=android", "--scope=runtime", "--exclude-tests"), capture = true)
    val graph = runtimeGraph(graphText)
    val tools = AndroidTools(androidSdk(root))
    val backend = ToolchainBackend(root, graph, resourceAars(root, tools))
    val loader = variant == "release"
    backend.build(module, variant, loader)
    if ("--compile-only" in flags) return
    val workspace = state.resolve("runs/${UUID.randomUUID()}").apply { mkdirs() }
    val raw = backend.apk(module, variant)
    check(tools.sdk.canonicalFile == backend.sdk(module, variant).canonicalFile) { "Toolchain SDK and packaging SDK differ" }
    val manifest = ApkManifest(tools, workspace)
    val rewritten = manifest.rewrite(raw, plan, loader, true, request.getProperty("project.name"))
    val additions = linkedMapOf<String, ByteArray>()
    additions["aboutlibraries.json"] = collectLicenses(root, graph.included, maven)
    val nativeRoot = root.resolve("jniLibs/arm64-v8a")
    listOf("libmihomo.so", "libpreview.so", "libmihomocore.so", "liboverride.so", "libcompat.so", "libloader.so").forEach {
        require(nativeRoot.resolve(it).isFile) { "Required native input missing: $it; run scripts/native-build.py first" }
    }
    nativeRoot.listFiles().orEmpty().filter { it.isFile && it.extension == "so" }.forEach { additions["lib/arm64-v8a/${it.name}"] = it.readBytes() }
    if (request.getProperty("geo.bundle", "false").toBooleanStrict()) {
        listOf("geoip.metadb.xz", "geosite.dat.xz", "ASN.mmdb.xz").forEach { name ->
            val file = root.resolve("build/generated/assets/geo/$name")
            require(file.isFile) { "Required builtin geo input missing: $file" }
            additions["assets/$name"] = file.readBytes()
        }
    }
    val prepared = workspace.resolve("prepared.apk")
    rewriteApk(rewritten, prepared, additions) { name ->
        name.startsWith("META-INF/") || name.endsWith(".kotlin_module") || name.endsWith(".kotlin_builtins") ||
            (name.startsWith("lib/") && name.substringAfterLast('/').startsWith("libjavet") || name.endsWith("BundleMRS.7z"))
    }
    val signing = signingIdentity(root, tools, variant, "--allow-unsigned" in flags)
    val ready = workspace.resolve("ready.apk")
    if (loader) {
        val loaderArtifact = backend.loaderArtifact(variant)
        val runtime = maven.artifact("org.lsposed.hiddenapibypass", "hiddenapibypass", "6.2", "aar")
        val dex = workspace.resolve("loader-dex")
        LoaderDexBuilder(tools.sdk, workspace.resolve("loader-work")).build(loaderArtifact, listOf(runtime), 26, dex)
        ApkPacker(tools.sdk, workspace.resolve("packer"), dex.resolve("classes.dex"), "com.github.yumeyucca.yumebox.App", "androidx.core.app.CoreComponentFactory", signing).pack(prepared, ready)
        verifyPackedPayload(ready)
    } else {
        tools.finish(prepared, ready, signing)
    }
    tools.verify(ready, signing)
    val applicationId = "com.github.yumeyucca.yumebox"
    manifest.verify(ready, plan, applicationId, loader)
    val name = plan.apkFileName("arm64-v8a")
    val output = root.resolve("output_apk/$variant/$name").apply { parentFile.mkdirs() }
    val temporary = File.createTempFile("verified-", ".apk", output.parentFile)
    ready.copyTo(temporary, overwrite = true)
    Files.move(temporary.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    output.resolveSibling(output.name + ".json").writeText(buildJsonObject {
        put("module", module); put("variant", variant); put("geo", plan.geoSegment)
        put("applicationId", applicationId); put("versionCode", plan.versionCode); put("versionName", plan.versionName)
        put("signed", signing != null); put("sha256", sha256(output)); put("toolchain", "0.13.0")
    }.toString())
    println("Verified APK: ${output.path}")
}

private fun signingIdentity(root: File, tools: AndroidTools, variant: String, allowUnsigned: Boolean): ApkSigning? {
    val file = root.resolve("signing.properties")
    if (file.isFile) {
        val properties = loadProperties(file)
        fun required(key: String) = requireNotNull(properties.getProperty(key)) { "Missing signing property $key" }
        val store = root.resolve(properties.getProperty("keystore.path", "release.keystore"))
        require(store.isFile) { "Signing keystore not found: $store" }
        return ApkSigning(store, required("key.alias"), required("keystore.password"), required("key.password"))
    }
    if (allowUnsigned) return null
    require(variant == "debug") { "Release signing is required; provide signing.properties or explicitly pass --allow-unsigned" }
    val debug = File(System.getProperty("user.home"), ".android/debug.keystore")
    if (!debug.isFile) {
        debug.parentFile.mkdirs()
        tools.run(listOf(File(System.getProperty("java.home"), "bin/keytool").path, "-genkeypair", "-noprompt", "-keystore", debug.path,
            "-storepass", "android", "-keypass", "android", "-alias", "androiddebugkey", "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000", "-dname", "CN=Android Debug,O=Android,C=US"))
    }
    return ApkSigning(debug, "androiddebugkey", "android", "android")
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
