fun git(vararg arguments: String): String =
    providers.exec {
        commandLine("git", *arguments)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()

val tagPrefix = extra["versionTagPrefix"] as String
val fallbackVersion = extra["fallbackVersion"] as String
val versionPattern = Regex(Regex.escape(tagPrefix) + "-v([0-9]+)\\.([0-9]+)\\.([0-9]+)")
val headTags = git("tag", "--points-at", "HEAD").lineSequence().filter(String::isNotBlank).toList()
val exactVersion = headTags.firstNotNullOfOrNull { versionPattern.matchEntire(it) }
val latestVersion = git(
    "for-each-ref",
    "--merged=HEAD",
    "--sort=-creatordate",
    "--format=%(refname:short)",
    "refs/tags",
).lineSequence().filter(String::isNotBlank).firstNotNullOfOrNull { versionPattern.matchEntire(it) }

val numericVersion = when {
    exactVersion != null -> exactVersion.groupValues.drop(1).joinToString(".")
    latestVersion != null -> {
        val (major, minor, patch) = latestVersion.groupValues.drop(1).map(String::toInt)
        "$major.$minor.${patch + 1}"
    }
    else -> fallbackVersion
}

val branch = providers.environmentVariable("GITHUB_HEAD_REF").orNull
    ?.takeIf(String::isNotBlank)
    ?: providers.environmentVariable("GITHUB_REF_NAME").orNull?.takeIf(String::isNotBlank)
    ?: git("branch", "--show-current").ifBlank { "detached" }
val qualifier = branch.replace('/', '_').replace(Regex("[^A-Za-z0-9_.-]"), "_")

extra["gitVersion"] = if (exactVersion != null) numericVersion else "$numericVersion-$qualifier-SNAPSHOT"
