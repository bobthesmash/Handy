pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven(url = uri("https://xdcobra.github.io/maven"))
            }
            filter {
                includeGroup("com.xdcobra.sherpa")
            }
        }
    }
}

rootProject.name = "Handy"
include(
    ":app",
    ":core:common",
    ":core:audio",
    ":core:persistence",
    ":feature:wakeword",
    ":feature:asr",
    ":feature:voiceid",
    ":feature:nlu",
    ":feature:nlu-llm",
    ":feature:actions",
    ":feature:tts",
    ":feature:ui",
    ":wear",
)
