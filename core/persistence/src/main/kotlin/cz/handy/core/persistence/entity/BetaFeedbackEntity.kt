package cz.handy.core.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Lokální záznam beta zpětné vazby; nic se automaticky neodesílá ([F4-T06]). */
@Entity(tableName = "beta_feedback")
data class BetaFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAtEpochMillis: Long,
    /** 1–5 celková spokojenost testem. */
    val satisfactionStars: Int,
    val messageText: String,
)
