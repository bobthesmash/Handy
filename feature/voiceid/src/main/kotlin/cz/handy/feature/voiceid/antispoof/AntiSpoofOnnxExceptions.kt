package cz.handy.feature.voiceid.antispoof

import java.util.Locale

/**
 * Model v assets navrhl vysokou pravděpodobnost spoof vstupu oproti [thresholdUsed].
 */
class AntiSpoofRejectedException(
    val spoofProbability: Float,
    val thresholdUsed: Float,
) : IllegalStateException(
        "Anti-spoof: podezření na přehraný nebo syntetický vstup (" +
            "P(spoof)=" +
            String.format(Locale.US, "%.2f", spoofProbability) +
            ", limit " +
            String.format(Locale.US, "%.2f", thresholdUsed) +
            ").",
    )

class AntiSpoofInferenceException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
