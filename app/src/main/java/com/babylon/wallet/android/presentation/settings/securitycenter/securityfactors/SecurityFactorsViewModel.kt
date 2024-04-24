package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.extensions.factorSourceIdString
import rdx.works.profile.domain.GetFactorSourcesWithAccountsUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ledgerFactorSources
import javax.inject.Inject

@Suppress("MagicNumber")
@HiltViewModel
class SecurityFactorsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getFactorSourcesWithAccountsUseCase: GetFactorSourcesWithAccountsUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase
) : StateViewModel<SecurityFactorsUiState>() {

    override fun initialState(): SecurityFactorsUiState = SecurityFactorsUiState(
        settings = persistentSetOf(
            SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(0, false),
            SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets(0)
        )
    )

    init {
        viewModelScope.launch {
            combine(
                getFactorSourcesWithAccountsUseCase(),
                getProfileUseCase.ledgerFactorSources,
                getEntitiesWithSecurityPromptUseCase(),
            ) { deviceFactorSources, ledgerFactorSources, entitiesWithSecurityPrompts ->
                val factorSourcesIds = deviceFactorSources.map { it.deviceFactorSource.id.body.value }
                val anyEntityNeedRecovery = entitiesWithSecurityPrompts.any { entityWithSecurityPrompt ->
                    entityWithSecurityPrompt.prompt == SecurityPromptType.NEEDS_RESTORE &&
                        factorSourcesIds.contains(entityWithSecurityPrompt.entity.factorSourceIdString)
                }
                SecurityFactorsUiState(
                    settings = persistentSetOf(
                        SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(deviceFactorSources.size, anyEntityNeedRecovery),
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
