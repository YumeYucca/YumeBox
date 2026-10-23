package dev.yume.packer

import com.android.aapt.Resources
import dev.yume.build.BuildPlan
import java.io.File
import java.util.zip.ZipFile

class ApkManifest(private val tools: AndroidTools, private val workspace: File) {
    fun rewrite(input: File, plan: BuildPlan, loader: Boolean, mainApp: Boolean, label: String): File {
        val proto = workspace.resolve("manifest-proto.apk")
        tools.run(listOf(tools.aapt2, "convert", "--output-format", "proto", "-o", proto.path, input.path))
        val manifest = ZipFile(proto).use { zip -> Resources.XmlNode.parseFrom(zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) }
        fun edit(node: Resources.XmlNode): Resources.XmlNode {
            if (!node.hasElement()) return node
            val element = node.element.toBuilder()
            if (element.name == "manifest") {
                element.androidAttribute("versionCode", 0x0101021b, plan.versionCode.toString(), Resources.Item.newBuilder().setPrim(Resources.Primitive.newBuilder().setIntDecimalValue(plan.versionCode)).build())
                element.stringAttribute("versionName", 0x0101021c, plan.versionName)
            }
            if (element.name == "application") {
                if (mainApp) {
                    element.stringAttribute("name", 0x01010003, if (loader) "com.github.yumeyucca.yumebox.loader.LoaderApplication" else "com.github.yumeyucca.yumebox.App")
                    element.stringAttribute("appComponentFactory", 0x0101057a, if (loader) "com.github.yumeyucca.yumebox.loader.LoaderComponentFactory" else "androidx.core.app.CoreComponentFactory")
                    element.stringAttribute("label", 0x01010001, label)
                }
                element.androidAttribute("extractNativeLibs", 0x010104ea, "true", Resources.Item.newBuilder().setPrim(Resources.Primitive.newBuilder().setBooleanValue(true)).build())
            }
            val children = element.childList.map(::edit)
            element.clearChild().addAllChild(children)
            return node.toBuilder().setElement(element).build()
        }
        val edited = workspace.resolve("manifest-edited-proto.apk")
        rewriteApk(proto, edited, mapOf("AndroidManifest.xml" to edit(manifest).toByteArray()))
        return workspace.resolve("manifest-binary.apk").also {
            tools.run(listOf(tools.aapt2, "convert", "--output-format", "binary", "-o", it.path, edited.path))
        }
    }

    fun verify(apk: File, plan: BuildPlan, applicationId: String, loader: Boolean) {
        val proto = workspace.resolve("verified-manifest.apk")
        tools.run(listOf(tools.aapt2, "convert", "--output-format", "proto", "-o", proto.path, apk.path))
        val manifest = ZipFile(proto).use { zip -> Resources.XmlNode.parseFrom(zip.getInputStream(zip.getEntry("AndroidManifest.xml"))).element }
        check(manifest.attributeList.single { it.name == "package" }.value == applicationId) { "Wrong APK identity" }
        check(manifest.attributeList.single { it.name == "versionCode" }.compiledItem.prim.intDecimalValue == plan.versionCode) { "Wrong APK versionCode" }
        check(manifest.attributeList.single { it.name == "versionName" }.value == plan.versionName) { "Wrong APK versionName" }
        val application = manifest.childList.single { it.hasElement() && it.element.name == "application" }.element
        check(application.attributeList.single { it.name == "extractNativeLibs" }.compiledItem.prim.booleanValue) { "Native library extraction must remain enabled" }
        if (loader) {
            check(application.attributeList.single { it.name == "name" }.value == "com.github.yumeyucca.yumebox.loader.LoaderApplication") { "Missing loader Application" }
            check(application.attributeList.single { it.name == "appComponentFactory" }.value == "com.github.yumeyucca.yumebox.loader.LoaderComponentFactory") { "Missing loader ComponentFactory" }
        }
    }
}

private fun Resources.XmlElement.Builder.stringAttribute(name: String, id: Int, value: String) =
    androidAttribute(name, id, value, Resources.Item.newBuilder().setStr(Resources.String.newBuilder().setValue(value)).build())

private fun Resources.XmlElement.Builder.androidAttribute(name: String, id: Int, value: String, item: Resources.Item) {
    val index = attributeList.indexOfFirst { it.namespaceUri == "http://schemas.android.com/apk/res/android" && it.name == name }
    val attribute = (if (index >= 0) getAttribute(index).toBuilder() else Resources.XmlAttribute.newBuilder())
        .setNamespaceUri("http://schemas.android.com/apk/res/android").setName(name).setResourceId(id).setValue(value).setCompiledItem(item)
    if (index >= 0) setAttribute(index, attribute) else addAttribute(attribute)
}
