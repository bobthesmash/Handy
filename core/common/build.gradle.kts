plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
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
