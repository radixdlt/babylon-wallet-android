package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.securityproblems.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.SecurityFactorsSettingsItem.SecurityFactorCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
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

    override fun initialState(): State = State(securityFactorSettingItems = currentSecurityFactorsSettings)

    init {
        viewModelScope.launch {
            getSecurityProblemsUseCase()
                .flowOn(defaultDispatcher)
                .collectLatest { securityProblems ->
                    if (securityProblems.isNotEmpty()) {
                        val firstCategory = currentSecurityFactorsSettings.keys.first()
                        val updatedSecurityFactorsSettings = currentSecurityFactorsSettings.put(
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
                            it.copy(securityFactorSettingItems = currentSecurityFactorsSettings)
                        }
                    }
                }
        }
    }

    data class State(
        val securityFactorSettingItems: ImmutableMap<SecurityFactorCategory, ImmutableSet<SecurityFactorsSettingsItem>>
    ) : UiState

    companion object {
        val currentSecurityFactorsSettings = if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
            persistentMapOf(
                SecurityFactorCategory.Own to persistentSetOf(SecurityFactorsSettingsItem.BiometricsPin(persistentSetOf())),
                SecurityFactorCategory.Hardware to persistentSetOf(
                    SecurityFactorsSettingsItem.ArculusCard,
                    SecurityFactorsSettingsItem.LedgerNano
                ),
                SecurityFactorCategory.Information to persistentSetOf(
                    SecurityFactorsSettingsItem.Password,
                    SecurityFactorsSettingsItem.Passphrase
                )
            )
        } else {
            persistentMapOf(
                SecurityFactorCategory.Own to persistentSetOf(SecurityFactorsSettingsItem.BiometricsPin(persistentSetOf())),
                SecurityFactorCategory.Hardware to persistentSetOf(
                    SecurityFactorsSettingsItem.LedgerNano
                )
            )
        }
    }
}
