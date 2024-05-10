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
        val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.NEEDS_BACKUP) }
        val entitiesNeedingRestore = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.NEEDS_RESTORE) }
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.securityState.factorSourceId }
        val factorSourceIdsNeedRecovery = entitiesNeedingRestore.map { it.entity.securityState.factorSourceId }
        mutableSetOf<SecurityProblem>().apply {
            factorSourceIdsNeedRecovery.forEach {
                add(SecurityProblem.EntitiesNeedRecovery(it))
            }
            factorSourceIdsNeedBackup.forEach { factorSourceId ->
                val accountsNeedBackup = entitiesNeedingBackup.count {
                    it.entity is ProfileEntity.AccountEntity && it.entity.securityState.factorSourceId == factorSourceId
                }
                val personasNeedBackup = entitiesNeedingBackup.count {
                    it.entity is ProfileEntity.PersonaEntity && it.entity.securityState.factorSourceId == factorSourceId
                }
                add(
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = accountsNeedBackup,
                        personasNeedBackup = personasNeedBackup
                    )
                )
                if (backupState.isWarningVisible) {
                    add(SecurityProblem.BackupNotWorking)
                }
            }
        }.toSet()
    }
}
