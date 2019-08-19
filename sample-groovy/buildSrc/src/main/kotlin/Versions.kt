import kotlin.String
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version.
 */
object Versions {
  const val okhttp: String = "3.12.1" // available: "4.1.0"

  const val okio: String = "2.0.0" // available: "2.3.0"

  const val de_fayard_buildsrcversions_gradle_plugin: String = "0.4.0"

  const val io_vertx_vertx_plugin_gradle_plugin: String = "0.3.1" // available: "0.8.0"

  const val vertx_core: String = "none"// No version. See buildSrcVersions#23

  const val vertx_stack_depchain: String = "3.6.2" // available: "4.0.0-milestone1"

  /**
   *
   * See issue 19: How to update Gradle itself?
   * https://github.com/jmfayard/buildSrcVersions/issues/19
   */
  const val gradleLatestVersion: String = "5.6"

  const val gradleCurrentVersion: String = "5.5.1"
}

/**
 * See issue #47: how to update buildSrcVersions itself
 * https://github.com/jmfayard/buildSrcVersions/issues/47
 */
val PluginDependenciesSpec.buildSrcVersions: PluginDependencySpec
  inline get() =
      id("de.fayard.buildSrcVersions").version(Versions.de_fayard_buildsrcversions_gradle_plugin)