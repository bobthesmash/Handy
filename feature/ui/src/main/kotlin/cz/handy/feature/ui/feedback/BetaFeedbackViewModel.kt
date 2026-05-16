package cz.handy.feature.ui.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.handy.core.persistence.HandyDatabase
import cz.handy.core.persistence.HandyLocalTelemetry
import cz.handy.core.persistence.LocalTelemetryPreferences
import cz.handy.core.persistence.entity.BetaFeedbackEntity
import cz.handy.feature.asr.SherpaStreamingRecognizerHolder
import cz.handy.feature.asr.decodeMono16StoredUtterance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BetaFeedbackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val db = HandyDatabase.getInstance(application)

    private val telemetry =
        HandyLocalTelemetry(application, LocalTelemetryPreferences(application))

    private val sherpaBorrow = SherpaStreamingRecognizerHolder(application)

    private val _statusLine = MutableStateFlow<String?>(null)
    val statusLine: StateFlow<String?> = _statusLine.asStateFlow()

    private val _transcriptionBusy = MutableStateFlow(false)
    val transcriptionBusy: StateFlow<Boolean> = _transcriptionBusy.asStateFlow()

    private val _recentSaved = MutableStateFlow<List<BetaFeedbackEntity>>(emptyList())
    val recentSaved: StateFlow<List<BetaFeedbackEntity>> = _recentSaved.asStateFlow()

    init {
        refreshRecentSaved()
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(BetaFeedbackViewModel::class.java))
            return BetaFeedbackViewModel(application) as T
        }
    }

    fun consumeStatus() {
        _statusLine.value = null
    }

    fun refreshRecentSaved() {
        viewModelScope.launch(Dispatchers.IO) {
            val rows = db.betaFeedbackDao().listRecentDescending(RECENT_FEEDBACK_QUERY_LIMIT)
            withContext(Dispatchers.Main.immediate) {
                _recentSaved.value = rows
            }
        }
    }

    suspend fun transcribeLocally(pcmMono16Le: ShortArray): String =
        withContext(Dispatchers.Default) {
            _transcriptionBusy.value = true
            try {
                val rec = sherpaBorrow.acquire()
                if (rec == null) {
                    ""
                } else {
                    rec.decodeMono16StoredUtterance(pcmMono16Le)
                }
            } finally {
                sherpaBorrow.release()
                _transcriptionBusy.value = false
            }
        }

    fun saveFeedback(
        stars: Int,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val text = message.trim()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.betaFeedbackDao().insert(
                    BetaFeedbackEntity(
                        createdAtEpochMillis = System.currentTimeMillis(),
                        satisfactionStars = stars.coerceIn(1, 5),
                        messageText = text,
                    ),
                )
                telemetry.recordBetaFeedbackSaved(stars.coerceIn(1, 5))
                val rows =
                    db.betaFeedbackDao().listRecentDescending(RECENT_FEEDBACK_QUERY_LIMIT)
                withContext(Dispatchers.Main.immediate) {
                    _recentSaved.value = rows
                    _statusLine.value = null
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = e.message ?: e.toString()
                    _statusLine.value = msg
                    onFailure(msg)
                }
            }
        }
    }

    companion object {
        const val RECENT_FEEDBACK_QUERY_LIMIT = 15
    }
}
