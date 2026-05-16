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



import java.io.File



/**

 * Single entry pro CI (`ciHandy`): ktlint, detekt, Android lint, testy v subprojektech, `assembleDebug`.
 * Úkol `ciHandyFull` navíc `:app:assembleRelease` — shoda s GitHub workflow.
 */

gradle.projectsEvaluated {

    val checkHandyOnnxDevAssets =
        tasks.register("checkHandyOnnxDevAssets") {
            group = "verification"
            description =
                "Non-failing: logs WARN lines when ONNX assets listed in READMEs are missing under feature/*/src/main/assets."
            doLast {
                val repoRoot = rootProject.layout.projectDirectory.asFile.path
                fun warnMissing(label: String, relativeUnixPath: String) {
                    val f = File(repoRoot).resolve(relativeUnixPath.replace('/', File.separatorChar))
                    if (!f.isFile) {
                        logger.warn("[HandyBundledOnnx missing] {} → {}", label, relativeUnixPath)
                    }
                }
                warnMissing(
                    "ECAPA embedding ONNX",
                    "feature/voiceid/src/main/assets/voiceid/ecapa_embedding.onnx",
                )
                warnMissing(
                    "Silero VAD ONNX",
                    "feature/voiceid/src/main/assets/voiceid/silero_vad.onnx",
                )
                warnMissing(
                    "Anti-spoof ONNX (volitelný)",
                    "feature/voiceid/src/main/assets/voiceid/anti_spoof.onnx",
                )
                val sherpaDir = "feature/asr/src/main/assets/asr/cs_zipformer_small"
                listOf("tokens.txt", "encoder.onnx", "decoder.onnx", "joiner.onnx").forEach {
                    warnMissing("Sherpa zipformer «$it»", "$sherpaDir/$it")
                }
            }
        }

    val ciHandy =
        tasks.register("ciHandy") {

            group = "verification"

            dependsOn(checkHandyOnnxDevAssets)

            description =
                "Aggregates ktlintCheck, detekt, lintDebug, JVM/Android unit tests, and :app:assembleDebug. " +
                    "GitHub Actions also runs `:app:assembleRelease` separately (R8 sanity)."

            subprojects.forEach { sub ->

                sub.tasks.findByName("ktlintCheck")?.let { dependsOn(it) }

                sub.tasks.findByName("detekt")?.let { dependsOn(it) }

                sub.tasks.findByName("lintDebug")?.let { dependsOn(it) }

                sub.tasks.findByName("test")?.let { dependsOn(it) }

                sub.tasks.findByName("testDebugUnitTest")?.let { dependsOn(it) }

            }

            dependsOn(project(":app").tasks.named("assembleDebug"))
        }

    tasks.register("ciHandyFull") {
        group = "verification"
        description =
            "Runs `ciHandy` plus `:app:assembleRelease` — stejný rozsah lokálně jako kombinovaný GitHub CI job."
        dependsOn(ciHandy)
        dependsOn(project(":app").tasks.named("assembleRelease"))
    }

}

