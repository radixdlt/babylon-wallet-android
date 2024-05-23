package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.radixdlt.sargon.extensions.id
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.ledgerFactorSources
import rdx.works.profile.domain.GetFactorSourcesWithAccountsUseCase
import rdx.works.profile.domain.GetProfileUseCase
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
            SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(0, false, false),
            SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets(0)
        )
    )

    init {
        viewModelScope.launch {
            combine(
                getFactorSourcesWithAccountsUseCase(),
                getProfileUseCase.flow.map { it.ledgerFactorSources },
                getEntitiesWithSecurityPromptUseCase(),
            ) { deviceFactorSources, ledgerFactorSources, entitiesWithSecurityPrompts ->
                val factorSourcesIds = deviceFactorSources.map { it.deviceFactorSource.id }
                val anyEntityNeedRecovery = entitiesWithSecurityPrompts.any { entityWithSecurityPrompt ->
                    entityWithSecurityPrompt.prompts.contains(SecurityPromptType.NEEDS_RESTORE) &&
                        entityWithSecurityPrompt.entity.securityState.factorSourceId in factorSourcesIds
                }
                val anyEntitySeedPhraseNotWrittenDown = entitiesWithSecurityPrompts.any { entityWithSecurityPrompt ->
                    entityWithSecurityPrompt.prompts.contains(SecurityPromptType.NEEDS_BACKUP) &&
                        entityWithSecurityPrompt.entity.securityState.factorSourceId in factorSourcesIds
                }
                SecurityFactorsUiState(
                    settings = persistentSetOf(
                        SettingsItem.SecurityFactorsSettingsItem.SeedPhrases(
                            deviceFactorSources.size,
                            anyEntityNeedRecovery,
                            anyEntitySeedPhraseNotWrittenDown
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
