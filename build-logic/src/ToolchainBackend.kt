package dev.yume.build

import java.io.File
import java.security.MessageDigest

class ToolchainBackend(private val root: File, graph: RuntimeGraph, private val resourceAars: List<ResourceAar>) {
    private val excludedNames = graph.excluded.flatMap { coordinate ->
        val classifier = coordinate.classifier.takeIf(String::isNotEmpty)?.let { "-$it" }.orEmpty()
        listOf("${coordinate.artifact}-${coordinate.version}$classifier.jar", "${coordinate.artifact}-${coordinate.version}$classifier.aar")
    }.sorted()
    // 0.13.0 has no desugaring/exclusion DSL. This is an internal-backend shim,
    // not a project build: orchestration, manifests, native payloads and signing stay Kotlin.
    private val initScript = """
        import org.gradle.api.artifacts.FileCollectionDependency
        import java.security.MessageDigest
        import java.util.zip.ZipFile
        import java.util.zip.ZipOutputStream
        import java.util.zip.ZipEntry
        def excludedNames = [${excludedNames.joinToString(",") { "'" + it.replace("'", "\\'") + "'" }}] as Set
        def resourceAars = [${resourceAars.joinToString(",") { "'" + it.module + "':'" + it.file.invariantSeparatorsPath.replace("'", "\\'") + "'" }.ifEmpty { ":" }}]
        def overlayAar = { File original, File resource, File directory ->
            directory.mkdirs()
            synchronized (directory.absolutePath.intern()) {
                def digest = MessageDigest.getInstance('SHA-256')
                digest.update(original.bytes)
                digest.update(resource.bytes)
                def output = new File(directory, original.name.substring(0, original.name.length() - 4) + '-' + digest.digest().encodeHex().toString() + '.aar')
                if (output.isFile()) return output
                def entries = [:]
                new ZipFile(original).withCloseable { zip -> zip.entries().each { entry -> if (!entry.directory) entries[entry.name] = zip.getInputStream(entry).bytes } }
                new ZipFile(resource).withCloseable { zip -> zip.entries().each { entry -> if (!entry.directory && entry.name != 'classes.jar') entries[entry.name] = zip.getInputStream(entry).bytes } }
                def temporary = File.createTempFile('overlay-', '.aar', directory)
                new ZipOutputStream(new FileOutputStream(temporary)).withCloseable { zip -> entries.each { name, bytes -> def entry = new ZipEntry(name); entry.time = 315532800000L; zip.putNextEntry(entry); zip.write(bytes); zip.closeEntry() } }
                java.nio.file.Files.move(temporary.toPath(), output.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                return output
            }
        }
        allprojects { p ->
            p.plugins.withId('com.android.application') {
                p.extensions.getByName('android').compileOptions.coreLibraryDesugaringEnabled = true
                p.dependencies.add('coreLibraryDesugaring', 'com.android.tools:desugar_jdk_libs:2.1.5')
                if (p.rootDir.path.contains('_prepareAndroid')) resourceAars.values().each { p.dependencies.add('implementation', p.files(it)) }
            }
            p.configurations.configureEach { configuration ->
                configuration.withDependencies { dependencies ->
                    def changes = []
                    dependencies.findAll { it instanceof FileCollectionDependency }.each { dependency ->
                        def original = dependency.files.files
                        def kept = original.findAll { !excludedNames.contains(it.name) }.collect { file ->
                            def module = file.name.endsWith('-jvm.aar') ? file.name.substring(0, file.name.length() - '-jvm.aar'.length()) : null
                            if (!p.rootDir.path.contains('_prepareAndroid') && module != null && resourceAars.containsKey(module) && file.toURI().path.contains('/tasks/_' + module + '_aarAndroid')) {
                                return overlayAar(file, new File(resourceAars[module]), new File(p.layout.buildDirectory.get().asFile, 'yumebox-aars'))
                            }
                            return file
                        }
                        if (kept.toSet() != original.toSet()) changes.add([dependency, kept])
                    }
                    changes.each { change ->
                        dependencies.remove(change[0])
                        if (!change[1].isEmpty()) dependencies.add(p.dependencies.create(p.files(change[1])))
                    }
                }
                configuration.resolutionStrategy.eachDependency { details ->
                    def group = details.requested.group
                    if (group == 'com.google.protobuf') details.useVersion('3.25.9')
                    if (group == 'org.bouncycastle') details.useVersion('1.86')
                    if (group == 'io.netty') details.useVersion('4.1.136.Final')
                    if (group.startsWith('tools.jackson')) details.useVersion('3.1.8')
                }
            }
        }
    """.trimIndent()
    private val fingerprint = MessageDigest.getInstance("SHA-256").digest(("0.13.0/resource-symbols-v1/nontransitive-true/nonfinal-true\n" + initScript).toByteArray()).take(8).joinToString("") { "%02x".format(it) }
    val buildRoot: File = root.resolve("build/toolchain-$fingerprint")
    private val gradleHome = root.resolve(".kotlin/yumebox/gradle-home")

    fun build(module: String, variant: String, loader: Boolean) {
        require(root.resolve("kotlin.bat").readText().contains("set kotlin_cli_version=0.13.0")) { "Update the Android output adapter before changing Toolchain version" }
        gradleHome.resolve("init.d/yumebox.gradle").apply { parentFile.mkdirs(); writeText(initScript) }
        gradleHome.resolve("gradle.properties").writeText(listOf("org.gradle.jvmargs=-Xmx8g -XX:MaxMetaspaceSize=1g", "org.gradle.caching=true", "android.nonTransitiveRClass=true", "android.nonFinalResIds=true", "").joinToString(System.lineSeparator()))
        println("Toolchain backend: $fingerprint; excluded runtime artifacts: ${excludedNames.joinToString()}")
        // The backend provisions its own SDK when ANDROID_HOME is unset; prefer the machine's
        // local.properties location so local runs do not download an unlicensed duplicate.
        val environment = mutableMapOf("GRADLE_USER_HOME" to gradleHome.path)
        if (System.getenv("ANDROID_HOME").isNullOrBlank()) {
            loadProperties(root.resolve("local.properties")).getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let { environment["ANDROID_HOME"] = it }
        }
        val arguments = mutableListOf("build", "--module=$module", "--variant=$variant", "--build-dir=${buildRoot.path}")
        if (loader) arguments += "--module=pack"
        toolchain(root, arguments, environment)
    }

    fun apk(module: String, variant: String): File {
        val suffix = variant.replaceFirstChar(Char::uppercaseChar)
        val directory = buildRoot.resolve("tasks/_${module}_buildAndroid$suffix")
        val candidates = directory.listFiles().orEmpty().filter { it.isFile && it.extension == "apk" }
        require(candidates.size == 1) { "Expected one exported APK for $module/$variant in $directory, found ${candidates.size}; update the pinned adapter" }
        return candidates.single()
    }

    fun sdk(module: String, variant: String): File {
        val suffix = variant.replaceFirstChar(Char::uppercaseChar)
        val local = loadProperties(buildRoot.resolve("tasks/_${module}_prepareAndroid$suffix/gradle-project/local.properties"))
        return File(requireNotNull(local.getProperty("sdk.dir")) { "Toolchain Android SDK handoff unavailable" })
    }

    fun loaderArtifact(variant: String): File {
        val suffix = variant.replaceFirstChar(Char::uppercaseChar)
        return buildRoot.resolve("tasks/_pack_aarAndroid$suffix/pack-jvm.aar").also {
            require(it.isFile) { "Toolchain loader AAR handoff changed: $it" }
        }
    }
}

fun toolchain(root: File, arguments: List<String>, environment: Map<String, String> = emptyMap(), capture: Boolean = false): String {
    val command = if (System.getProperty("os.name").startsWith("Windows")) listOf("cmd.exe", "/d", "/c", root.resolve("kotlin.bat").path) else listOf("sh", root.resolve("kotlin").path)
    val builder = ProcessBuilder(command + arguments).directory(root).redirectError(ProcessBuilder.Redirect.INHERIT)
    builder.environment().putAll(environment)
    if (!capture) builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    val process = builder.start()
    val output = if (capture) process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } else ""
    check(process.waitFor() == 0) { "Kotlin Toolchain command failed: ${arguments.first()}" }
    return output
}
