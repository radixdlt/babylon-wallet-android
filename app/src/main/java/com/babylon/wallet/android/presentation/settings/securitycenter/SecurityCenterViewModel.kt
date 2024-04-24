package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<SecurityCenterViewModel.SecurityCenterUiState>() {

    override fun initialState(): SecurityCenterUiState {
        return SecurityCenterUiState()
    }

    init {
        viewModelScope.launch {
            combine(
                getEntitiesWithSecurityPromptUseCase(),
                getBackupStateUseCase()
            ) { entitiesWithSecurityPrompts, backupState ->
                val anyEntityNeedRestore = entitiesWithSecurityPrompts.any { it.prompt == SecurityPromptType.NEEDS_RESTORE }
                val anyEntityNeedBackup = entitiesWithSecurityPrompts.any { it.prompt == SecurityPromptType.NEEDS_BACKUP }
                val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompt == SecurityPromptType.NEEDS_BACKUP }
                val accountsNeedRecovery = entitiesNeedingBackup.filterIsInstance<Network.Account>().size
                val personasNeedRecovery = entitiesNeedingBackup.filterIsInstance<Network.Persona>().size
                SecurityCenterUiState(
                    securityFactorsState = mutableSetOf<SecurityPromptType>().apply {
                        if (anyEntityNeedRestore) add(SecurityPromptType.NEEDS_RESTORE)
                        if (anyEntityNeedBackup) add(SecurityPromptType.NEEDS_BACKUP)
                    },
                    backupState = backupState,
                    accountsNeedRecovery = accountsNeedRecovery,
                    personasNeedRecovery = personasNeedRecovery,
                )
            }.collect { securityFactorsUiState ->
                _state.update { securityFactorsUiState }
            }
        }
    }

    data class SecurityCenterUiState(
        val securityFactorsState: Set<SecurityPromptType>? = null,
        val backupState: BackupState? = null,
        val entitiesWithSecurityPromptUseCase: List<GetEntitiesWithSecurityPromptUseCase> = emptyList(),
        val accountsNeedRecovery: Int = 0,
        val personasNeedRecovery: Int = 0
    ) : UiState
}
