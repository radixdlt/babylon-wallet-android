package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import java.time.Duration
import javax.inject.Inject

class NPSSurveyUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
) {

    suspend fun incrementTransactionCompleteCounter() {
        val currentCounter = preferencesManager.transactionCompleteCounter().first()
        preferencesManager.updateTransactionCompleteCounter(currentCounter + 1)
    }

    fun npsSurveyState(): Flow<NPSSurveyState> = combine(
        preferencesManager.lastNPSSurveyInstant,
        preferencesManager.transactionCompleteCounter()
    ) { lastNPSSurveyInstant, transactionCompleteCounter ->
        if (lastNPSSurveyInstant != null) {
            // Survey has been shown already, check last show time and compare with 3 months gap
            val duration = Duration.between(lastNPSSurveyInstant, InstantGenerator())
            if (duration.toDays() < OUTSTANDING_TIME_DAYS) {
                NPSSurveyState.InActive
            } else {
                NPSSurveyState.Active
            }
        } else {
            // Survey has never been show, check transaction count
            if (transactionCompleteCounter >= TRANSACTION_COMPLETE_COUNTER) {
                NPSSurveyState.Active
            } else {
                NPSSurveyState.InActive
            }
        }
    }

    companion object {
        private const val OUTSTANDING_TIME_DAYS = 30 * 3 // Approximately 3 months
        private const val TRANSACTION_COMPLETE_COUNTER = 10
    }
}

sealed class NPSSurveyState {
    data object Active : NPSSurveyState()
    data object InActive : NPSSurveyState()
}
