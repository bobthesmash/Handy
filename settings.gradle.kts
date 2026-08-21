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

// Android Studio: vytvoří local.properties při prvním Sync, pokud chybí a SDK je na výchozí cestě.
run {
    val localProps = settingsDir.resolve("local.properties")
    if (!localProps.isFile) {
        val sdkFromEnv =
            sequenceOf(
                System.getenv("ANDROID_SDK_ROOT"),
                System.getenv("ANDROID_HOME"),
            ).firstOrNull { !it.isNullOrBlank() && java.io.File(it).isDirectory }
        val defaultSdk =
            sdkFromEnv
                ?: when {
                    System.getProperty("os.name").lowercase().contains("win") ->
                        "${System.getProperty("user.home")}\\AppData\\Local\\Android\\Sdk"
                    System.getProperty("os.name").lowercase().contains("mac") ->
                        "${System.getProperty("user.home")}/Library/Android/sdk"
                    else -> "${System.getProperty("user.home")}/Android/Sdk"
                }
        val sdkDir = java.io.File(defaultSdk)
        if (sdkDir.isDirectory) {
            val escaped = sdkDir.path.replace("\\", "\\\\")
            localProps.writeText(
                """
                sdk.dir=$escaped

                # Volitelné: wake word — https://console.picovoice.ai/
                # picovoice.access.key=

                """.trimIndent(),
            )
            println("Handy: vytvořen local.properties (sdk.dir=${sdkDir.path})")
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
