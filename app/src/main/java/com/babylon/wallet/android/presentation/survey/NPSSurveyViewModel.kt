package com.babylon.wallet.android.presentation.survey

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.NPSSurveyRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class NPSSurveyViewModel @Inject constructor(
    private val nPSSurveyRepository: NPSSurveyRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus
) : StateViewModel<NPSSurveyViewModel.State>(), OneOffEventHandler<NPSSurveyEvent> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            preferencesManager.updateLastNPSSurveyInstant(InstantGenerator())
        }
    }

    override fun initialState(): State = State(
        scores = (0..10).map {
            Selectable(data = SurveyScore(score = it), selected = false)
        }.toImmutableList()
    )

    fun onReasonChanged(reason: String) {
        _state.update { it.copy(reason = reason) }
    }

    fun onScoreClick(selectedScore: SurveyScore) {
        _state.update {
            it.copy(
                scores = state.value.scores.map { score ->
                    if (selectedScore == score.data) {
                        Selectable(score.data, true)
                    } else {
                        Selectable(score.data, false)
                    }
                }.toPersistentList()
            )
        }
    }

    fun onSubmitClick() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            nPSSurveyRepository.submitSurveyResponse(state.value.scoreSelected, state.value.reason.orEmpty())
            appEventBus.sendEvent(AppEvent.NPSSurveySubmitted)
            finish()
        }
    }

    private suspend fun finish() {
        _state.update { it.copy(isLoading = false) }
        sendEvent(NPSSurveyEvent.Close)
    }

    fun onBackPress() {
        appScope.launch {
            nPSSurveyRepository.submitSurveyResponse("", "")
            appEventBus.sendEvent(AppEvent.NPSSurveySubmitted)
        }
        viewModelScope.launch {
            finish()
        }
    }

    data class State(
        val reason: String? = null,
        val isLoading: Boolean = false,
        val scores: ImmutableList<Selectable<SurveyScore>> = persistentListOf()
    ) : UiState {

        val isSubmitButtonEnabled: Boolean
            get() = scores.any { it.selected }

        val scoreSelected: String
            get() = scores.find { it.selected }?.data?.score.toString()
    }
}

sealed interface NPSSurveyEvent : OneOffEvent {
    data object Close : NPSSurveyEvent
}

data class SurveyScore(
    val score: Int
)
