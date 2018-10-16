package jmfayard.github.io

import com.squareup.kotlinpoet.*
import java.io.File
import java.util.*

internal val LibsClassName = "Libs"
internal val VersionsClassName = "Versions"


/**
 * We don't want to use meaningless generic names like Libs.core
 *
 * Found many inspiration for bad names here https://developer.android.com/jetpack/androidx/migrate
 * **/
val MEANING_LESS_NAMES: List<String> = listOf(
    "common", "core", "core-testing", "testing", "runtime", "extensions",
    "compiler", "migration", "db", "rules", "runner", "monitor", "loader",
    "media", "print", "io", "media", "collection", "gradle"
)

val GITIGNORE = """
.gradle/
build/
"""

val GRADLE_KDOC = """
  To update Gradle, edit the wrapper file at path:
     ./gradle/wrapper/gradle-wrapper.properties
"""

val GENERATED_BY_SYNCLIBS = """
    Generated by [gradle-kotlin-dsl-libs](https://github.com/jmfayard/gradle-kotlin-dsl-libs)

    Run again
      `$ ./gradlew syncLibs`
    to update this file
    """.trimIndent()


const val INITIAL_BUILD_GRADLE_KTS = """
plugins {
    `kotlin-dsl`
}
repositories {
    jcenter()
}
        """

fun helpMessageBefore(jsonInput: File): String = """
          Done running $ ./gradlew dependencyUpdates   # com.github.ben-manes:gradle-versions-plugin
          Reading info about your dependencies from ${jsonInput.absolutePath}
          """.trimIndent()


fun helpMessageAfter(
    fileExisted: Boolean,
    dependencies: List<Dependency>,
    file: File
): String {
    val createdOrupdated = if (fileExisted) "Updated file" else "Created file"
    val path = file.absolutePath
    val someDependency = random(dependencies)?.escapedName ?: "xxx"

    return """
            $createdOrupdated $path

            It contains meta-data about all your dependencies, including available updates and links to the website

            Its content is available in all your build.gradle and build.gradle.kts

            // build.gradle or build.gradle.kts
            dependencies {
               Libs.$someDependency
            }

            Run again the task any time you add a dependency or want to check for updates
               $ ./gradlew syncLibs

            """.trimIndent()
}


@Suppress("LocalVariableName")
fun kotlinpoet(versions: List<Dependency>, gradleConfig: GradleConfig): KotlinPoetry {

    val versionsProperties: List<PropertySpec> = versions.map { d: Dependency ->
        constStringProperty(
            name = d.escapedName,
            initializer = CodeBlock.of("%S %L", d.version, d.versionInformation())
        )
    }
    val libsProperties: List<PropertySpec> = versions.map { d ->
        constStringProperty(
            name = d.escapedName,
            initializer = CodeBlock.of("%S + Versions.%L", "${d.group}:${d.name}:", d.escapedName),
            kdoc = d.projectUrl?.let { url ->
                CodeBlock.of("[%L website](%L)", d.name, url)
            }
        )
    }

    val gradleProperties: List<PropertySpec> = listOf(
        constStringProperty("runningVersion", gradleConfig.running.version),
        constStringProperty("currentVersion", gradleConfig.current.version),
        constStringProperty("nightlyVersion", gradleConfig.nightly.version),
        constStringProperty("releaseCandidate", gradleConfig.releaseCandidate.version)
    )

    val Gradle: TypeSpec = TypeSpec.objectBuilder("Gradle")
        .addProperties(gradleProperties)
        .addKdoc(GRADLE_KDOC)
        .build()

    val Versions: TypeSpec = TypeSpec.objectBuilder("Versions")
        .addKdoc(GENERATED_BY_SYNCLIBS)
        .addType(Gradle).addProperties(versionsProperties)
        .build()


    val Libs = TypeSpec.objectBuilder("Libs")
        .addKdoc(GENERATED_BY_SYNCLIBS)
        .addProperties(libsProperties)
        .build()


    val LibsFile = FileSpec.builder("", LibsClassName)
        .addType(Libs)
        .build()

    val VersionsFile = FileSpec.builder("", VersionsClassName)
        .addType(Versions)
        .build()

    return KotlinPoetry(Libs = LibsFile, Versions = VersionsFile)

}


fun SyncLibsTask.Companion.parseGraph(graph: DependencyGraph): List<Dependency> {
    val dependencies: List<Dependency> = graph.current + graph.exceeded + graph.outdated + graph.unresolved

    val map = mutableMapOf<String, Dependency>()
    for (d: Dependency in dependencies) {
        val key = escapeName(d.name)
        val fdqnName = escapeName("${d.group}_${d.name}")


        if (key in MEANING_LESS_NAMES) {
            d.escapedName = fdqnName
        } else if (map.containsKey(key)) {
            d.escapedName = fdqnName

            // also use FDQN for the dependency that conflicts with this one
            val other = map[key]!!
            other.escapedName = escapeName("${other.group}_${other.name}")
            println("Will use FDQN for ${other.escapedName}")
        } else {
            map[key] = d
            d.escapedName = key
        }
    }
    return dependencies
        .distinctBy { it.escapedName }
        .sortedBy { it.escapedName }

}


fun constStringProperty(name: String, initializer: CodeBlock, kdoc: CodeBlock? = null) =
    PropertySpec.builder(name, String::class)
        .addModifiers(KModifier.CONST)
        .initializer(initializer)
        .apply {
            if (kdoc != null) addKdoc(kdoc)
        }.build()


fun constStringProperty(name: String, initializer: String, kdoc: CodeBlock? = null) =
    constStringProperty(name, CodeBlock.of("%S", initializer))


fun escapeName(name: String): String {
    val escapedChars = listOf('-', '.', ':')
    return buildString {
        for (c in name) {
            append(if (c in escapedChars) '_' else c.toLowerCase())
        }
    }
}

fun Dependency.versionInformation(): String {
    return when {
        latest.isNullOrBlank().not() -> "// exceed the version found: $latest"
        reason.isNullOrBlank().not() -> this.unresolvedReason()!!
        available != null -> available.displayComment()
        else -> "// up-to-date"
    }
}


fun Dependency.unresolvedReason() : String? {
    val shorterReason = reason?.lines()?.take(4)?.joinToString(separator = "\n") ?: ""
    return when {
        shorterReason.isBlank() -> ""
        shorterReason.contains("Could not find any matches") -> "// No update information. Is this dependency available on jcenter or mavenCentral?"
        else -> "\n/* $shorterReason \n.... */"
    }
}

fun AvailableDependency.displayComment(): String = when {
    release.isNullOrBlank().not() -> "// available: release=$release"
    milestone.isNullOrBlank().not() -> "// available: milestone=$milestone"
    integration.isNullOrBlank().not() -> "// available: integration=$integration"
    else -> "// " + this.toString()
}


private val random = Random()

private fun random(deps: List<Dependency>): Dependency? {
    val index = random.nextInt(deps.size)
    return if (deps.isEmpty()) null else deps[index]
}

