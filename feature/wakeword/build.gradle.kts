import java.util.Properties



plugins {

    alias(libs.plugins.android.library)

    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.ktlint)

    alias(libs.plugins.detekt)
}

android {

    namespace = "cz.handy.feature.wakeword"

    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {

        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        val lp = Properties()

        val localFile =
            rootProject.layout.projectDirectory
                .file("local.properties")
                .asFile

        if (localFile.exists()) {

            localFile.reader().use { reader -> lp.load(reader) }
        }

        val keyRaw =

            lp.getProperty("picovoice.access.key")?.trim().orEmpty().ifEmpty {

                project
                    .findProperty("PICVOICE_ACCESS_KEY")
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            }

        val escaped = keyRaw.replace("\\", "\\\\").replace("\"", "\\\"")

        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"$escaped\"")
    }

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_17

        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {

        buildConfig = true
    }

    lint {

        abortOnError = true
    }
}

kotlin {

    jvmToolchain(17)
}

dependencies {

    implementation(project(":core:audio"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.porcupine.android)

    implementation(libs.openwakeword.android)

    testImplementation(libs.kotlin.test.junit)
}

ktlint {

    version.set("1.5.0")
}

detekt {

    buildUponDefaultConfig = true

    allRules = false

    config.from(
        files(
            sequenceOf(
                rootProject.projectDir.resolve("config/detekt/detekt.yml"),
                rootProject.projectDir.parentFile.resolve("config/detekt/detekt.yml"),
            ).firstOrNull { it.exists() }
                ?: error("Could not locate config/detekt/detekt.yml (Gradle root=${rootProject.projectDir})"),
        ),
    )
}
