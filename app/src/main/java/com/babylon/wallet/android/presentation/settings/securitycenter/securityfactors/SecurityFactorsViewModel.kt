package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetSecurityProblemsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.deviceFactorSources
import rdx.works.core.sargon.ledgerFactorSources
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class SecurityFactorsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getSecurityProblemsUseCase: GetSecurityProblemsUseCase
) : StateViewModel<SecurityFactorsUiState>() {

    override fun initialState(): SecurityFactorsUiState = SecurityFactorsUiState(
        settings = persistentSetOf(
            SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(0, persistentSetOf()),
            SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets(0)
        )
    )

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow,
                getSecurityProblemsUseCase()
            ) { profile, securityProblems ->
                val ledgerFactorSources = profile.ledgerFactorSources
                val deviceFactorSources = profile.deviceFactorSources
                SecurityFactorsUiState(
                    settings = persistentSetOf(
                        SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(
                            deviceFactorSources.size,
                            securityProblems.toPersistentSet()
                        ),
                        SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets(ledgerFactorSources.size)
                    )
                )
            }.collect { securityFactorsUiState ->
                _state.update { securityFactorsUiState }
            }
        }
    }
}

data class SecurityFactorsUiState(
    val settings: ImmutableSet<SettingsItem.SecurityFactorsSettingsItem>
) : UiState
