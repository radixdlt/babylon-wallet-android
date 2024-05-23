package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.SecurityProblem
import com.radixdlt.sargon.extensions.ProfileEntity
import kotlinx.coroutines.flow.combine
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

class GetSecurityProblemsUseCase @Inject constructor(
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getBackupStateUseCase: GetBackupStateUseCase
) {

    operator fun invoke() = combine(
        getEntitiesWithSecurityPromptUseCase(),
        getBackupStateUseCase()
    ) { entitiesWithSecurityPrompts, backupState ->
        val entitiesNeedingRecovery = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.NEEDS_RESTORE) }
        val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.NEEDS_BACKUP) }
        val factorSourceIdsNeedRecovery = entitiesNeedingRecovery.map { it.entity.securityState.factorSourceId }
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.securityState.factorSourceId }.toSet()
        val anyPersonaNeedRecovery = entitiesNeedingRecovery.any { it.entity is ProfileEntity.PersonaEntity }
        mutableSetOf<SecurityProblem>().apply {
            if (factorSourceIdsNeedRecovery.isNotEmpty()) {
                add(SecurityProblem.SeedPhraseNeedRecovery(anyPersonaNeedRecovery))
            }
            val accountsNeedBackup = entitiesNeedingBackup.count {
                it.entity is ProfileEntity.AccountEntity && factorSourceIdsNeedBackup.contains(it.entity.securityState.factorSourceId)
            }
            val personasNeedBackup = entitiesNeedingBackup.count {
                it.entity is ProfileEntity.PersonaEntity && factorSourceIdsNeedBackup.contains(it.entity.securityState.factorSourceId)
            }
            if (factorSourceIdsNeedBackup.isNotEmpty()) {
                add(
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = accountsNeedBackup,
                        personasNeedBackup = personasNeedBackup
                    )
                )
            }
            if (backupState.isWarningVisible) {
                add(SecurityProblem.BackupNotWorking)
            }
        }.toSet()
    }
}
