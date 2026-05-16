package cz.handy.feature.voiceid.confirm

import android.content.Context
import cz.handy.feature.voiceid.ecapa.EcapaOnnxSpeakerEmbeddingExtractor
import cz.handy.feature.voiceid.storage.SpeakerEmbeddingEncryptedStore
import cz.handy.feature.voiceid.verify.DualThresholdSpeakerVerifier
import cz.handy.feature.voiceid.verify.VerificationThresholdStore
import cz.handy.feature.voiceid.verify.VerificationVerdict
import java.util.Locale

/**
 * Druhý krok u destruktivních intentů ([F1-T16]): nová utterance musí dát **≥ T_high** vůči uloženému embeddingu.
 */
class DestructiveConfirmVoiceVerifier(
    context: Context,
    private val embeddingStore: SpeakerEmbeddingEncryptedStore = SpeakerEmbeddingEncryptedStore(context),
    private val thresholdStore: VerificationThresholdStore = VerificationThresholdStore(context),
    private val extractor: EcapaOnnxSpeakerEmbeddingExtractor = EcapaOnnxSpeakerEmbeddingExtractor(context),
) {
    fun releaseOnnxResources() {
        extractor.releaseSession()
    }

    /**
     * Kosínový embedding tahu proti uloženému centroidu — bez mapování na výsledek SMS ([DualThresholdSpeakerVerifier]).
     */
    fun evaluateTurnAgainstStoredProfile(pcmMono16Le: ShortArray): Result<VerificationVerdict> {
        val ref =
            embeddingStore.peekSynchronized()
                ?: return Result.failure(
                    IllegalStateException(
                        "Nejdřív dokončete zápis hlasu (sekce Zápis hlasu).",
                    ),
                )
        val sample =
            extractor.embedPcm16(pcmMono16Le).getOrElse {
                return Result.failure(it)
            }
        val thresholds = thresholdStore.read()
        val (_, verdict) = DualThresholdSpeakerVerifier.verify(sample, ref, thresholds)
        return Result.success(verdict)
    }

    /**
     * @param pcmMono16Le vzorky z Enrollment / stejný formát jako [EcapaOnnxSpeakerEmbeddingExtractor.embedPcm16].
     */
    fun verifyDestructiveConfirmUtterance(pcmMono16Le: ShortArray): Result<Unit> {
        val ref =
            embeddingStore.peekSynchronized()
                ?: return Result.failure(
                    IllegalStateException(
                        "Nejdřív dokončete zápis hlasu (sekce Zápis hlasu).",
                    ),
                )
        val sample =
            extractor.embedPcm16(pcmMono16Le).getOrElse {
                return Result.failure(it)
            }
        val thresholds = thresholdStore.read()
        val (score, verdict) = DualThresholdSpeakerVerifier.verify(sample, ref, thresholds)
        val scoreStr = String.format(Locale.US, "%.3f", score)
        val tHighStr = String.format(Locale.US, "%.2f", thresholds.cosineHigh)
        return when (verdict) {
            VerificationVerdict.StrongAccept -> Result.success(Unit)
            VerificationVerdict.Uncertain ->
                Result.failure(
                    IllegalStateException(
                        "Potvrzení hlasem musí být jisté (T_high). Skóre $scoreStr je pod $tHighStr — zkus to znovu zřetelně.",
                    ),
                )
            VerificationVerdict.Reject ->
                Result.failure(
                    IllegalStateException(
                        "Hlas neodpovídá profilu (skóre $scoreStr).",
                    ),
                )
        }
    }

    companion object {
        /** Index souboru `cache/.../enrollment/phrase_*.pcm` pro confirm klip (neměří se krok enrollment UI). */
        const val CLIP_PHRASE_INDEX: Int = 801
    }
}
