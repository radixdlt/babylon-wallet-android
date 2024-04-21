package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.SecurityProblem
import kotlinx.coroutines.flow.combine
import rdx.works.profile.data.model.extensions.factorSourceIdString
import rdx.works.profile.data.model.pernetwork.Network
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
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.factorSourceIdString }
        val factorSourceIdsNeedRecovery = entitiesNeedingRestore.map { it.entity.factorSourceIdString }
        mutableSetOf<SecurityProblem>().apply {
            factorSourceIdsNeedRecovery.forEach {
                add(SecurityProblem.EntitiesNeedRecovery(it))
            }
            factorSourceIdsNeedBackup.forEach { factorSourceId ->
                val accountsNeedBackup = entitiesNeedingBackup.count {
                    it.entity is Network.Account && it.entity.factorSourceIdString == factorSourceId
                }
                val personasNeedBackup = entitiesNeedingBackup.count {
                    it.entity is Network.Persona && it.entity.factorSourceIdString == factorSourceId
                }
                add(
                    SecurityProblem.EntitiesNeedBackup(
                        factorSourceID = factorSourceId,
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
