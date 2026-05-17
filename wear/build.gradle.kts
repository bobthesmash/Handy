plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "cz.handy.wear"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "cz.handy.wear"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.0.1-wear"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
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
