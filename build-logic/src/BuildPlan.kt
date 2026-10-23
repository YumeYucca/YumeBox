package dev.yume.build

data class BuildRequest(
    val productName: String,
    val baseVersionName: String,
    val versionCodeEpoch: Int,
    val buildNumber: String? = null,
    val buildHash: String? = null,
    val buildBranch: String? = null,
    val geoBundle: Boolean = false,
    val outputPrefix: String? = null,
    val outputChannel: String? = null,
    val outputTail: String? = null,
)

data class BuildPlan(
    val versionCode: Int,
    val versionName: String,
    val outputPrefix: String,
    val geoSegment: String,
    val outputChannel: String?,
    val outputTail: String?,
) {
    fun apkFileName(abi: String): String =
        buildList {
            add(outputPrefix)
            add(geoSegment)
            if (abi != "arm64-v8a") add(abi)
            outputChannel?.let(::add)
            outputTail?.let(::add)
        }.joinToString("-") + ".apk"
}

fun planBuild(request: BuildRequest): BuildPlan {
    require(request.versionCodeEpoch > 0) { "versionCode epoch must be positive" }
    val number = request.buildNumber.nonBlank()?.toInt() ?: 0
    require(number >= 0) { "build.number must not be negative" }
    val hash = request.buildHash.nonBlank()?.take(8)
    // Keep in sync with reusable-prepare-publish.yml.
    val branch =
        request.buildBranch?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')?.nonBlank()
    return BuildPlan(
        versionCode = Math.addExact(request.versionCodeEpoch, number),
        versionName =
            hash?.let { listOfNotNull(request.baseVersionName, branch, it).joinToString(".") }
                ?: request.baseVersionName,
        outputPrefix = request.outputPrefix.nonBlank() ?: request.productName,
        geoSegment = if (request.geoBundle) "builtin" else "external",
        outputChannel = request.outputChannel.nonBlank(),
        outputTail = request.outputTail.nonBlank(),
    )
}

private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
