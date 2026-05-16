package cz.handy.feature.actions.sms

/**
 * Tvrdá pojistka: po UI + **hlasovém** potvrzení (≥ T_high, viz [F1-T16]) předat `confirmedByUser = true`.
 * NLU má `requiresConfirm` na intentu SEND_SMS.
 */
internal fun requireDestructiveSmsUserConfirmation(confirmedByUser: Boolean): Result<Unit> =
    if (confirmedByUser) {
        Result.success(Unit)
    } else {
        Result.failure(
            IllegalStateException(
                "SMS bez explicitního potvrzení uživatelem.",
            ),
        )
    }
