pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Keepfit"

include(":app")
include(":core:model")
include(":core:database")
include(":core:designsystem")
include(":core:media")
include(":feature:workouts")
