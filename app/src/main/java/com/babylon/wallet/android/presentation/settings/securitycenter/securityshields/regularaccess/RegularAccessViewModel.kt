package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.securityshield.SecurityShieldBuilderClient
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.common.toCompactInstanceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceInstanceCard
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegularAccessViewModel @Inject constructor(
    private val securityShieldBuilderClient: SecurityShieldBuilderClient
) : StateViewModel<RegularAccessViewModel.State>() {

    init {
        initSelection()
    }

    override fun initialState(): State = State()

    fun onNumberOfFactorsClick() {
        val factorCount = _state.value.thresholdFactors.size

        _state.update {
            it.copy(
                selectNumberOfFactors = State.SelectNumberOfFactors(
                    current = it.numberOfFactors,
                    items = (listOf(State.NumberOfFactors.All) + (1 until factorCount).map { value -> State.NumberOfFactors.Count(value) })
                        .toPersistentList()
                )
            )
        }
    }

    fun onNumberOfFactorsSelect(numberOfFactors: State.NumberOfFactors) {
        viewModelScope.launch {
            val factorCount = _state.value.thresholdFactors.size
            val newThreshold = securityShieldBuilderClient.setThreshold(numberOfFactors.toThreshold(factorCount))

            _state.update {
                it.copy(
                    selectNumberOfFactors = null,
                    numberOfFactors = State.NumberOfFactors.fromThreshold(newThreshold, factorCount)
                )
            }
        }
    }

    fun onNumberOfFactorsSelectionDismiss() {
        _state.update { it.copy(selectNumberOfFactors = null) }
    }

    fun onAddThresholdFactorClick() {
        // Show factor source selector
    }

    fun onRemoveThresholdFactorClick(card: FactorSourceInstanceCard) {
        viewModelScope.launch {
            securityShieldBuilderClient.removeFactor(card.id)
            initSelection()
        }
    }

    fun onAddOverrideClick() {
        // Show factor source selector
    }

    fun onAddLoginFactorClick() {
        // Show factor source selector
    }

    fun onRemoveLoginFactorClick() {
        // Remove login factor
    }

    fun onRemoveOverrideFactorClick(card: FactorSourceInstanceCard) {
        viewModelScope.launch {
            securityShieldBuilderClient.removeFactor(card.id)
            initSelection()
        }
    }

    fun onRemoveAllOverrideFactorsClick() {
        viewModelScope.launch {
            _state.value.overrideFactors.forEach {
                securityShieldBuilderClient.removeFactor(it.id)
            }
            initSelection()
        }
    }

    private fun initSelection() {
        viewModelScope.launch {
            val selection = securityShieldBuilderClient.getPrimaryRoleSelection()

            _state.update { state ->
                state.copy(
                    thresholdFactors = selection.thresholdFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                    overrideFactors = selection.overrideFactors.map { it.toCompactInstanceCard(true) }.toPersistentList(),
                    loginFactor = selection.loginFactor?.toCompactInstanceCard(true)
                )
            }
        }
    }

    data class State(
        val status: SelectedFactorSourcesForRoleStatus? = null,
        val numberOfFactors: NumberOfFactors = NumberOfFactors.All,
        val selectNumberOfFactors: SelectNumberOfFactors? = null,
        val thresholdFactors: PersistentList<FactorSourceInstanceCard> = persistentListOf(),
        val overrideFactors: PersistentList<FactorSourceInstanceCard> = persistentListOf(),
        val loginFactor: FactorSourceInstanceCard? = null,
        val isButtonEnabled: Boolean = false,
        val message: UiMessage? = null
    ) : UiState {

        data class SelectNumberOfFactors(
            val current: NumberOfFactors,
            val items: PersistentList<NumberOfFactors>
        )

        sealed interface NumberOfFactors {

            data object All : NumberOfFactors

            data class Count(
                val value: Int
            ) : NumberOfFactors

            fun toThreshold(factorCount: Int): Int = when (this) {
                is All -> factorCount
                is Count -> value
            }

            companion object {

                fun fromThreshold(threshold: Int, factorCount: Int): NumberOfFactors = when {
                    threshold == factorCount -> All
                    else -> Count(threshold)
                }
            }
        }
    }
}
