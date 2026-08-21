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

/** Sherpa-onnx 1.12.28 + ECAPA musí sdílet stejnou `libonnxruntime.so` (ORT 1.17.1). */
subprojects {
    configurations.configureEach {
        resolutionStrategy.force("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")
    }
}



/**

 * Single entry pro CI (`ciHandy`): ktlint, detekt, Android lint, testy v subprojektech, `assembleDebug`.
 * Úkol `ciHandyFull` navíc `:app:assembleRelease` — shoda s GitHub workflow.
 */

gradle.projectsEvaluated {
    val repoRoot = rootProject.layout.projectDirectory.asFile
    val voskMarker = repoRoot.resolve("feature/asr/src/main/assets/asr/vosk_cs_small/am/final.mdl")
    val sileroMarker = repoRoot.resolve("feature/voiceid/src/main/assets/voiceid/silero_vad.onnx")

    val downloadHandyOnnxDevAssets =
        tasks.register<Exec>("downloadHandyOnnxDevAssets") {
            group = "handy"
            description =
                "Stáhne Vosk CZ + Silero + Sherpa záloha + ECAPA (Python pro ECAPA). Ruční / CI doplnění."
            workingDir = repoRoot
            commandLine(
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                "scripts/download-handy-onnx-assets.ps1",
            )
        }

    val ensureHandyOnnxDevAssets =
        tasks.register<Exec>("ensureHandyOnnxDevAssets") {
            group = "handy"
            description =
                "Před buildem app: stáhne český Vosk + Silero VAD, pokud chybí (Windows, první build může trvat ~2 min)."
            workingDir = repoRoot
            commandLine(
                "powershell",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                "scripts/download-handy-onnx-assets.ps1",
                "-SkipSherpa",
                "-SkipEcapa",
            )
            onlyIf {
                val auto =
                    providers.gradleProperty("handy.autoDownloadAssets")
                        .orElse("true")
                        .get()
                        .toBoolean()
                val ci =
                    providers.environmentVariable("CI").isPresent ||
                        providers.environmentVariable("GITHUB_ACTIONS").isPresent
                auto &&
                    !ci &&
                    System.getProperty("os.name").lowercase().contains("win") &&
                    (!voskMarker.isFile || !sileroMarker.isFile)
            }
            outputs.files(voskMarker, sileroMarker)
        }

    val checkHandyOnnxDevAssets =
        tasks.register("checkHandyOnnxDevAssets") {
            group = "verification"
            description =
                "Non-failing: logs WARN lines when ONNX assets listed in READMEs are missing. Run downloadHandyOnnxDevAssets to fetch them."
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
                warnMissing(
                    "Vosk CS am/final.mdl (primární ASR)",
                    "feature/asr/src/main/assets/asr/vosk_cs_small/am/final.mdl",
                )
                val sherpaDir = "feature/asr/src/main/assets/asr/cs_zipformer_small"
                listOf("tokens.txt", "encoder.onnx", "decoder.onnx", "joiner.onnx").forEach {
                    warnMissing("Sherpa zipformer záloha «$it»", "$sherpaDir/$it")
                }
                warnMissing(
                    "MediaPipe LLM task (volitelný F5-T01)",
                    "feature/nlu-llm/src/main/assets/nlu_llm/gemma_hand_task.task",
                )
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

    project(":app").tasks.named("preBuild").configure {
        dependsOn(ensureHandyOnnxDevAssets)
    }

    tasks.register("ciHandyFull") {
        group = "verification"
        description =
            "Runs `ciHandy` plus `:app:assembleRelease` — stejný rozsah lokálně jako kombinovaný GitHub CI job."
        dependsOn(ciHandy)
        dependsOn(project(":app").tasks.named("assembleRelease"))
    }

}

