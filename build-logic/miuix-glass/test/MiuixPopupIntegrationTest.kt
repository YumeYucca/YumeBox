package dev.yume.build

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MiuixPopupIntegrationTest {
    private val root: File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { it.resolve("third_party/miuix/miuix-glass").isDirectory }

    /**
     * Covers [adaptMiuixTransformPopup], which rewrites upstream sources before they are compiled.
     *
     * Only generated output is asserted on: asserting on the text of hand-written product files
     * turns every refactor into a false failure.
     */
    @Test
    fun closingSpringCannotPassNegativeGeometryToPopupFrame() {
        val upstream =
            root.resolve(
                "third_party/miuix/miuix-glass/src/commonMain/kotlin/top/yukonga/miuix/kmp/glass/GlassTransformPopup.kt"
            ).readText()
        val adapted = adaptMiuixTransformPopup(upstream)
        assertTrue(adapted.contains("fun geometryProgress() = popupFractionWithBack(bounds.value, backProgress.value).coerceIn(0f, 1f)"))
        assertTrue(adapted.indexOf("if (anchor.containerBounds.isEmpty) return") < adapted.indexOf("DisposableEffect(active, anchor)"))
        assertTrue(adapted.contains("sizeFraction = geometryProgress()"))
        assertTrue(adapted.contains("if (!active) onDismissFinished?.invoke()"))
        assertTrue(upstream.contains("cornerRadius = startRadius + (endRadius - startRadius) * sizeFraction"))
    }
}
