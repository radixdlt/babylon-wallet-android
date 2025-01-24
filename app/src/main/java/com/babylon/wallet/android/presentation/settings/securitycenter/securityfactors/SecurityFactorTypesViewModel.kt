package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.FactorSourceKindsByCategory
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceKindsByCategoryUseCase
import com.babylon.wallet.android.domain.usecases.securityproblems.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.SecurityFactorTypeUiItem
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityFactorTypesViewModel @Inject constructor(
    getSecurityProblemsUseCase: GetSecurityProblemsUseCase,
    getFactorSourceKindsByCategoryUseCase: GetFactorSourceKindsByCategoryUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SecurityFactorTypesViewModel.State>() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val kindsByCategory = getFactorSourceKindsByCategoryUseCase()

            getSecurityProblemsUseCase()
                .flowOn(defaultDispatcher)
                .collectLatest { securityProblems ->
                    updateState(kindsByCategory, securityProblems)
                }
        }
    }

    private fun updateState(kindsByCategory: List<FactorSourceKindsByCategory>, securityProblems: Set<SecurityProblem>) {
        _state.update { state ->
            state.copy(
                items = kindsByCategory.map { (category, kinds) ->
                    val header = SecurityFactorTypeUiItem.Header(category)
                    val items = kinds.map { kind ->
                        SecurityFactorTypeUiItem.Item(
                            factorSourceKind = kind,
                            messages = getFactorSourceKindStatusMessages(kind, securityProblems).toPersistentList()
                        )
                    }

                    listOfNotNull(header) + items
                }.flatten().toPersistentList()
            )
        }
    }

    private fun getFactorSourceKindStatusMessages(
        factorSourceKind: FactorSourceKind,
        securityProblems: Set<SecurityProblem>
    ): List<FactorSourceStatusMessage> = if (factorSourceKind == FactorSourceKind.DEVICE) {
        securityProblems.mapNotNull { problem ->
            when (problem) {
                is SecurityProblem.EntitiesNotRecoverable -> FactorSourceStatusMessage.SecurityPrompt.EntitiesNotRecoverable
                is SecurityProblem.SeedPhraseNeedRecovery -> FactorSourceStatusMessage.SecurityPrompt.SeedPhraseNeedRecovery
                else -> null
            }
        }.toPersistentList()
    } else {
        persistentListOf()
    }

    data class State(
        val items: PersistentList<SecurityFactorTypeUiItem> = persistentListOf()
    ) : UiState
}
