package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.SecurityProblem
import com.radixdlt.sargon.extensions.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.domain.backup.GetCloudBackupStateUseCase
import javax.inject.Inject

class GetSecurityProblemsUseCase @Inject constructor(
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getCloudBackupStateUseCase: GetCloudBackupStateUseCase
) {

    operator fun invoke(): Flow<Set<SecurityProblem>> = combine(
        getEntitiesWithSecurityPromptUseCase(),
        getCloudBackupStateUseCase()
    ) { entitiesWithSecurityPrompts, cloudBackupState ->
        val entitiesNeedingRecovery = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.RECOVERY_REQUIRED) }
        val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.WRITE_DOWN_SEED_PHRASE) }
        val factorSourceIdsNeedRecovery = entitiesNeedingRecovery.map { it.entity.securityState.factorSourceId }
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.securityState.factorSourceId }.toSet()
        val anyPersonaNeedRecovery = entitiesNeedingRecovery.any { it.entity is ProfileEntity.PersonaEntity }

        mutableSetOf<SecurityProblem>().apply {
            if (cloudBackupState is CloudBackupState.Disabled) {
                add(SecurityProblem.BackupNotWorking.BackupDisabled(hasManualBackup = cloudBackupState.lastManualBackupTime != null))
            } else if (cloudBackupState is CloudBackupState.Enabled && cloudBackupState.hasAnyErrors) {
                add(SecurityProblem.BackupNotWorking.BackupServiceError)
            }
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
        }.toSet()
    }
}
