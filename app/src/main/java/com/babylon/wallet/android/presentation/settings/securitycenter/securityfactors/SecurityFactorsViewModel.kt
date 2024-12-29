package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.securityproblems.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import com.babylon.wallet.android.presentation.ui.composables.securityfactors.currentSecurityFactorTypeItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityFactorsViewModel @Inject constructor(
    getSecurityProblemsUseCase: GetSecurityProblemsUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SecurityFactorsViewModel.State>() {

    override fun initialState(): State = State(securityFactorSettingItems = currentSecurityFactorTypeItems)

    init {
        viewModelScope.launch {
            getSecurityProblemsUseCase()
                .flowOn(defaultDispatcher)
                .collectLatest { securityProblems ->
                    if (securityProblems.isNotEmpty()) {
                        val firstCategory = currentSecurityFactorTypeItems.keys.first()
                        val updatedSecurityFactorsSettings = currentSecurityFactorTypeItems.put(
                            key = firstCategory,
                            value = persistentSetOf(
                                SecurityFactorsSettingsItem.BiometricsPin(
                                    securityProblems = securityProblems.toPersistentSet()
                                )
                            )
                        )
                        _state.update {
                            it.copy(securityFactorSettingItems = updatedSecurityFactorsSettings)
                        }
                    } else {
                        _state.update {
                            it.copy(securityFactorSettingItems = currentSecurityFactorTypeItems)
                        }
                    }
                }
        }
    }

    data class State(
        val securityFactorSettingItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>
    ) : UiState
}
