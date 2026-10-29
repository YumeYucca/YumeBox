package dev.yume.build

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiuixPopupIntegrationTest {
    private val root: File = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .first { it.resolve("third_party/miuix/miuix-glass").isDirectory }

    @Test
    fun closingSpringCannotPassNegativeGeometryToPopupFrame() {
        val upstream = root.resolve("third_party/miuix/miuix-glass/src/commonMain/kotlin/top/yukonga/miuix/kmp/glass/GlassTransformPopup.kt").readText()
        val adapted = adaptMiuixTransformPopup(upstream)
        assertTrue(adapted.contains("fun geometryProgress() = popupFractionWithBack(bounds.value, backProgress.value).coerceIn(0f, 1f)"))
        assertTrue(adapted.indexOf("if (anchor.containerBounds.isEmpty) return") < adapted.indexOf("DisposableEffect(active, anchor)"))
        assertTrue(adapted.contains("sizeFraction = geometryProgress()"))
        assertTrue(adapted.contains("if (!active) onDismissFinished?.invoke()"))
        assertTrue(upstream.contains("cornerRadius = startRadius + (endRadius - startRadius) * sizeFraction"))
    }

    @Test
    fun menuAnchorIsAppliedToGlyphAndNeverToCapsule() {
        val renderer = root.resolve("ui/src/presentation/component/GlassTopActions.kt").readText()
            .substringAfter("private fun PillActionIcon")
        assertTrue(renderer.contains(".then(action.iconModifier)"), "Pill renderer must apply the glyph anchor modifier")
        val topBar = root.resolve("feature/proxy/src/presentation/screen/Proxy.kt").readText()
            .substringAfter("private fun ProxyTopBar")
        assertFalse(topBar.substringAfter("GlassTopActionPill(").substringBefore("actions =").contains("glassPopupAnchor("))
        assertTrue(topBar.contains("iconModifier = Modifier.glassPopupAnchor("))
        assertTrue(topBar.contains(".glassPopupAnchorContent(actionsMenuAnchor)"))
        val popup = root.resolve("feature/proxy/src/presentation/screen/node/NodeSortPopup.kt").readText()
        assertTrue(popup.contains("anchorAlpha = 0f"), "A bare glyph must not inherit a panel background/shadow at rest")
        assertFalse(popup.contains("delay(400)"), "The popup host must survive the entire spring exit")
        assertTrue(popup.contains("onDismissFinished = { if (!show) popupVisible.value = false }"))
    }

    @Test
    fun relocatedMenuStartsClosedAndRetainsPageBackdrop() {
        val source = root.resolve("feature/proxy/src/presentation/screen/node/NodeSortPopup.kt").readText()
        val menu = source.substringAfter("internal fun NodeActionsMenu(")
            .substringBefore("internal fun NodeSortPopupOverlay(")
        val host = menu.substringAfter("PopupLayout(")
        assertTrue(menu.indexOf("val backdrop = LocalTopBarBackdrop.current") < menu.indexOf("PopupLayout("))
        assertFalse(host.contains("LocalTopBarBackdrop.current"), "Relocated content must not lose its scene backdrop")
        assertTrue(host.contains("var expanded by remember { mutableStateOf(false) }"))
        assertTrue(host.contains("LaunchedEffect(show) { expanded = show }"))
        assertTrue(host.contains("show = expanded,"), "The library must observe false -> true to animate opening")
        assertTrue(host.contains("show = expanded && layoutSubmenu,"))
        assertTrue(host.contains("show = expanded && sortSubmenu,"))
        assertTrue(host.contains("backdrop = backdrop,"))
        assertTrue(menu.contains("val popupVisuals = GlassPopupDefaults.visuals()"))
    }

}
