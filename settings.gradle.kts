pluginManagement {
    repositories {
        // Use the standard repositories for plugin resolution. Earlier content filters
        // could prevent resolving some AGP plugin coordinates on some environments.
        google()
        mavenCentral()
        gradlePluginPortal()
        // fallback to explicit Google Maven URL if necessary
        maven {
            url = uri("https://maven.google.com")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Astronaut Stories"
include(":app")
 