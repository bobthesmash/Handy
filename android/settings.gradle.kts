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

@Suppress("UnstableApiUsage")
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
    versionCatalogs {
        create("libs") {
            val catalogFile = settingsDir.parentFile.resolve("gradle/libs.versions.toml")
            require(catalogFile.isFile) { "Missing version catalog $catalogFile (clone full repo)." }
            from(files(catalogFile))
        }
    }
}

rootProject.name = "Handy"

include(":app")
project(":app").projectDir = file("../app")

include(":core:common")
project(":core:common").projectDir = file("../core/common")

include(":core:audio")
project(":core:audio").projectDir = file("../core/audio")

include(":core:persistence")
project(":core:persistence").projectDir = file("../core/persistence")

include(":feature:wakeword")
project(":feature:wakeword").projectDir = file("../feature/wakeword")

include(":feature:asr")
project(":feature:asr").projectDir = file("../feature/asr")

include(":feature:voiceid")
project(":feature:voiceid").projectDir = file("../feature/voiceid")

include(":feature:nlu")
project(":feature:nlu").projectDir = file("../feature/nlu")

include(":feature:actions")
project(":feature:actions").projectDir = file("../feature/actions")

include(":feature:tts")
project(":feature:tts").projectDir = file("../feature/tts")

include(":feature:ui")
project(":feature:ui").projectDir = file("../feature/ui")
