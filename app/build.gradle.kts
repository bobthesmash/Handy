plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "cz.handy.app"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "cz.handy.app"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            pickFirsts += "**/libonnxruntime.so"
            pickFirsts += "**/libc++_shared.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:audio"))
    implementation(project(":feature:wakeword"))
    implementation(project(":feature:asr"))
    implementation(project(":feature:voiceid"))
    implementation(project(":feature:nlu"))
    implementation(project(":feature:actions"))
    implementation(project(":feature:tts"))
    implementation(project(":feature:ui"))

    // Tink/crypto transitives reference `@Immutable`; R8 needs the stubs on classpath.
    implementation(libs.google.error.prone.annotations)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

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
