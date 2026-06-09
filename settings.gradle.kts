pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Lê o token do local.properties
val localProps = java.util.Properties().also { props ->
    val f = file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}
val mapboxToken: String = localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = mapboxToken
            }
        }
    }
}

rootProject.name = "Atlas"
include(":app")
