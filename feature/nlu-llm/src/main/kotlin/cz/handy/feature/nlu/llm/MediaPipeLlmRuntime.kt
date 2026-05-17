package cz.handy.feature.nlu.llm

import android.app.Application
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

internal class MediaPipeLlmRuntime private constructor(
    private val inference: LlmInference,
    private val sessionOptions: LlmInferenceSession.LlmInferenceSessionOptions,
) {
    fun generate(prompt: String): String {
        LlmInferenceSession.createFromOptions(inference, sessionOptions).use { session ->
            session.addQueryChunk(prompt)
            return session.generateResponse()
        }
    }

    fun close() {
        inference.close()
    }

    companion object {
        private const val TAG = "HandyMediaPipeLlm"

        fun createIfPossible(
            application: Application,
            absoluteModelPath: String,
        ): MediaPipeLlmRuntime? =
            try {
                val infOpts =
                    LlmInference.LlmInferenceOptions
                        .builder()
                        .setModelPath(absoluteModelPath)
                        .setMaxTokens(512)
                        .build()
                val inference = LlmInference.createFromOptions(application, infOpts)
                val sessionOpts =
                    LlmInferenceSession.LlmInferenceSessionOptions
                        .builder()
                        .setTopK(40)
                        .setTopP(0.9f)
                        .setTemperature(0.15f)
                        .build()
                MediaPipeLlmRuntime(inference, sessionOpts)
            } catch (t: Throwable) {
                Log.w(TAG, "MediaPipe LLM init failed", t)
                null
            }
    }
}
