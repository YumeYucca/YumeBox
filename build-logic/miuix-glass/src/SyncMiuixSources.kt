package dev.yume.build

import java.io.File
import java.nio.file.Path
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction

@TaskAction
fun syncMiuixGlassSources(@Input repositoryDir: Path, @Output sourceDir: Path) {
    val repository = repositoryDir.toFile()
    val roots = listOf(
        repository.resolve("miuix-ui/src/commonMain/kotlin"),
        repository.resolve("miuix-ui/src/androidMain/kotlin"),
        repository.resolve("miuix-preference/src/commonMain/kotlin"),
        repository.resolve("miuix-glass/src/commonMain/kotlin"),
    )
    roots.forEach { require(it.isDirectory) { "Missing sparse Miuix source root: ${it.path}" } }

    val output = sourceDir.toFile()
    if (output.exists()) require(output.deleteRecursively()) { "Unable to clear generated Miuix sources: ${output.path}" }
    require(output.mkdirs() || output.isDirectory) { "Unable to create generated Miuix source directory: ${output.path}" }

    roots.forEach { root ->
        root.walkTopDown()
            .filter(File::isFile)
            .filter { it.extension == "kt" }
            .forEach { source ->
                val target = output.resolve(source.relativeTo(root).path)
                require(!target.exists()) { "Duplicate Miuix source path: ${target.relativeTo(output).path}" }
                target.parentFile.mkdirs()
                source.copyTo(target)
            }
    }

    val commonBridges = listOf(
        "top/yukonga/miuix/kmp/theme/DynamicColors.kt",
        "top/yukonga/miuix/kmp/theme/ProvideDisplayCutoutFix.kt",
        "top/yukonga/miuix/kmp/utils/PagerNonTouchScroll.kt",
        "top/yukonga/miuix/kmp/utils/WindowNavigationEventScope.kt",
    )
    commonBridges.forEach { relative ->
        val file = output.resolve(relative)
        val transformed = when (relative) {
            "top/yukonga/miuix/kmp/utils/PagerNonTouchScroll.kt" ->
                file.readText().replace("internal expect fun CompositionLocalConsumerModifierNode.pagerScrollConfig(): PagerScrollConfig\n", "")
            else -> file.readText().replace(Regex("@Composable\\s+expect fun [^\\n]+\\n"), "")
        }
        file.writeText(transformed)
    }

    listOf(
        "top/yukonga/miuix/kmp/theme/DynamicColors.android.kt",
        "top/yukonga/miuix/kmp/theme/ProvideDisplayCutoutFix.android.kt",
        "top/yukonga/miuix/kmp/utils/PagerNonTouchScroll.android.kt",
        "top/yukonga/miuix/kmp/utils/WindowNavigationEventScope.android.kt",
    ).forEach { relative ->
        val file = output.resolve(relative)
        file.writeText(file.readText().replace("internal actual fun ", "internal fun ").replace("actual fun ", "fun "))
    }

    val popup = output.resolve("top/yukonga/miuix/kmp/glass/GlassTransformPopup.kt")
    popup.writeText(adaptMiuixTransformPopup(popup.readText()))
}

/** Android integration: spring overshoot must never produce a negative panel or corner. */
internal fun adaptMiuixTransformPopup(source: String): String {
    val geometry = "fun geometryProgress() = popupFractionWithBack(bounds.value, backProgress.value)"
    val lifecycle = "    DisposableEffect(active, anchor) {"
    val measured = "    onMeasured: ((Size) -> Unit)? = null,"
    val active = "    val active = show || transition.currentState || transition.isRunning"
    require(listOf(geometry, lifecycle, measured, active).all(source::contains)) {
        "Miuix popup integration changed; review geometry and anchor lifecycle before updating"
    }
    return source
        .replace(geometry, "$geometry.coerceIn(0f, 1f)")
        .replace(lifecycle, "    if (anchor.containerBounds.isEmpty) return\n\n$lifecycle")
        .replace(measured, "$measured\n    onDismissFinished: (() -> Unit)? = null,")
        .replace(active, "$active\n    androidx.compose.runtime.LaunchedEffect(active) {\n        if (!active) onDismissFinished?.invoke()\n    }")
}
