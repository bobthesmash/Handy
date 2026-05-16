plugins {

    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.kotlin.compose.compiler) apply false

    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.hilt.android) apply false

    alias(libs.plugins.ktlint) apply false

    alias(libs.plugins.detekt) apply false
}



/**

 * Single entry for CI (F0-T03): runs every subproject's ktlint, detekt, Android lint, unit tests, then :app:assembleDebug.

 */

gradle.projectsEvaluated {

    tasks.register("ciHandy") {

        group = "verification"

        description = "Aggregates ktlintCheck, detekt, lintDebug, JVM/Android unit tests, and :app:assembleDebug."

        subprojects.forEach { sub ->

            sub.tasks.findByName("ktlintCheck")?.let { dependsOn(it) }

            sub.tasks.findByName("detekt")?.let { dependsOn(it) }

            sub.tasks.findByName("lintDebug")?.let { dependsOn(it) }

            sub.tasks.findByName("test")?.let { dependsOn(it) }

            sub.tasks.findByName("testDebugUnitTest")?.let { dependsOn(it) }
        }

        dependsOn(project(":app").tasks.named("assembleDebug"))
    }
}
