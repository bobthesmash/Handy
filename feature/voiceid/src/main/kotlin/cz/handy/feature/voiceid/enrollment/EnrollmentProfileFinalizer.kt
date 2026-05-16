package cz.handy.feature.voiceid.enrollment

import android.content.Context
import cz.handy.feature.voiceid.ecapa.EcapaOnnxSpeakerEmbeddingExtractor
import cz.handy.feature.voiceid.ecapa.SpeechbrainEcapaPreprocessor
import cz.handy.feature.voiceid.io.Pcm16LittleEndianIo
import cz.handy.feature.voiceid.storage.SpeakerEmbeddingEncryptedStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dokončí zápis majitele: načte `cache/enrollment/phrase_*.pcm`, ECAPA embeddingy → centroid → [SpeakerEmbeddingEncryptedStore].
 */
class EnrollmentProfileFinalizer(
    context: Context,
    private val extractor: EcapaOnnxSpeakerEmbeddingExtractor =
        EcapaOnnxSpeakerEmbeddingExtractor(context.applicationContext),
    private val embeddingStore: SpeakerEmbeddingEncryptedStore =
        SpeakerEmbeddingEncryptedStore(context.applicationContext),
) {
    private val app = context.applicationContext

    suspend fun finalizeEnrollmentClips(phraseCount: Int): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                if (phraseCount <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Nepovolený počet vět."))
                }

                val vectors = mutableListOf<FloatArray>()

                for (i in 0 until phraseCount) {
                    val f = EnrollmentClipRecorder.cachedPhraseFile(app, phraseIndex = i)
                    if (!f.isFile || f.length() <= 1L) {
                        return@withContext Result.failure(
                            IllegalStateException(
                                "Chybí nahrávka věty ${i + 1} (${f.name}). Všechny věty musí projít celým cyklem Zastavit.",
                            ),
                        )
                    }

                    val pcm =
                        try {
                            Pcm16LittleEndianIo.readMonoLeShorts(f)
                        } catch (e: Throwable) {
                            return@withContext Result.failure(e)
                        }

                    if (pcm.size < SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES) {
                        return@withContext Result.failure(
                            IllegalStateException(
                                "Věta ${i + 1}: audio je příliš krátké (${pcm.size} vzorků, " +
                                    "potřeba ≥ ${SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES}).",
                            ),
                        )
                    }

                    val emb =
                        extractor.embedPcm16(pcm).getOrElse {
                            return@withContext Result.failure(it)
                        }
                    vectors += emb
                }

                val centroid = EnrollmentEmbeddingCentroid.mergeEmbeddings(vectors)
                embeddingStore.replaceAll(centroid).getOrElse {
                    return@withContext Result.failure(it)
                }
                Result.success(Unit)
            } catch (err: Throwable) {
                Result.failure(err)
            } finally {
                extractor.releaseSession()
            }
        }
}
