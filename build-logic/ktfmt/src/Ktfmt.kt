package dev.yume.ktfmt

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.jetbrains.amper.plugins.TaskAction

/** ktfmt's `kotlinlangStyle()` with the project's 120 column limit. */
private val KotlinlangStyle =
    FormattingOptions(
        maxWidth = 120,
        blockIndent = 4,
        continuationIndent = 4,
        // Import cleanup is left to the compiler: ktfmt would drop imports referenced only from KDoc.
        removeUnusedImports = false,
    )

private val SkippedDirectories =
    setOf(
        "build",
        ".gradle",
        ".kotlin",
        "node_modules",
        "output_apk",
        "third_party",
    )

private fun isSourceRoot(directory: Path): Boolean = directory.name == "src" || directory.name.startsWith("src@")

/** Collects every Kotlin source under the project's `src` and `src@<platform>` directories. */
private fun kotlinSources(projectRootDir: Path): List<Path> {
    val sources = mutableListOf<Path>()

    fun visit(directory: Path) {
        Files.newDirectoryStream(directory).use { entries ->
            for (entry in entries) {
                if (!Files.isDirectory(entry)) continue
                if (isSourceRoot(entry)) {
                    Files.walk(entry).use { paths ->
                        paths
                            .filter { it.isRegularFile() && it.extension == "kt" }
                            .filter { path -> entry.relativize(path).none { it.toString() in SkippedDirectories } }
                            .forEach(sources::add)
                    }
                } else if (entry.name !in SkippedDirectories && !entry.name.startsWith(".")) {
                    visit(entry)
                }
            }
        }
    }
    visit(projectRootDir)
    return sources.sortedBy(Path::toString)
}

/**
 * Formats every Kotlin source of the project with ktfmt. With [write] disabled it only reports the files that differ
 * and fails when there are any.
 */
@TaskAction
fun formatKtfmt(
    projectRootDir: String,
    write: Boolean = false,
) {
    val root = Path(projectRootDir)
    if (!root.exists()) return
    var changed = 0
    var unparsable = 0
    val sources = kotlinSources(root)
    for (source in sources) {
        val original = source.readText()
        val formatted =
            try {
                Formatter.format(KotlinlangStyle, original)
            } catch (exception: Exception) {
                println(
                    "ktfmt: cannot format ${root.relativize(source).invariantSeparatorsPathString}: ${exception.message}"
                )
                unparsable++
                continue
            }
        if (formatted == original) continue
        changed++
        if (write) {
            source.writeText(formatted)
        } else {
            println("ktfmt: ${root.relativize(source).invariantSeparatorsPathString} needs formatting")
        }
    }
    val outcome = if (write) "reformatted" else "need formatting"
    println("ktfmt: ${sources.size} files scanned, $changed $outcome, $unparsable unparsable")
    if (!write && changed > 0) error("ktfmt: $changed file(s) are not formatted; run ./kotlin do formatKtfmt")
}
