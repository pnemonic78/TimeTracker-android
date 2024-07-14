import org.gradle.api.JavaVersion
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependency

object Android {
    object Version {
        const val compileSdk = 34
        const val minSdk = 23
        const val targetSdk = 34
    }
}

object Java {
    object Version {
        val jvm = JavaVersion.VERSION_11
    }
}

// Alias with only its id, and without its version.
fun PluginDependenciesSpec.aliasId(notation: Provider<PluginDependency>) =
    id(notation.get().pluginId)
