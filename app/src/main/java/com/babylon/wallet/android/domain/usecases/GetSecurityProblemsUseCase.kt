package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.SecurityProblem
import com.radixdlt.sargon.extensions.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.isHidden
import rdx.works.core.sargon.isNotHidden
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
        val entitiesNeedCloudBackup = entitiesWithSecurityPrompts.filter {
            it.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM) ||
                it.prompts.contains(SecurityPromptType.WALLET_NOT_RECOVERABLE) ||
                it.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED)
        }
        val entitiesNeedingRecovery = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.RECOVERY_REQUIRED) }
        val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.WRITE_DOWN_SEED_PHRASE) }
        val factorSourceIdsNeedRecovery = entitiesNeedingRecovery.map { it.entity.securityState.factorSourceId }
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.securityState.factorSourceId }.toSet()
        val isAnyActivePersonaAffected = entitiesNeedingRecovery.any { it.entity is ProfileEntity.PersonaEntity && it.entity.isNotHidden() }

        mutableSetOf<SecurityProblem>().apply {
            if (factorSourceIdsNeedRecovery.isNotEmpty()) {
                add(SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = isAnyActivePersonaAffected))
            }

            val accountsNeedBackup = entitiesNeedingBackup.filter {
                it.entity is ProfileEntity.AccountEntity && factorSourceIdsNeedBackup.contains(it.entity.securityState.factorSourceId)
            }
            val personasNeedBackup = entitiesNeedingBackup.filter {
                it.entity is ProfileEntity.PersonaEntity && factorSourceIdsNeedBackup.contains(it.entity.securityState.factorSourceId)
            }

            if (factorSourceIdsNeedBackup.isNotEmpty()) {
                add(
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = accountsNeedBackup.count { it.entity.isNotHidden() },
                        personasNeedBackup = personasNeedBackup.count { it.entity.isNotHidden() },
                        hiddenAccountsNeedBackup = accountsNeedBackup.count { it.entity.isHidden() },
                        hiddenPersonasNeedBackup = personasNeedBackup.count { it.entity.isHidden() }
                    )
                )
            }

            val activePersonasNeedCloudBackup = entitiesNeedCloudBackup.count { it.entity.isNotHidden() }
            if (cloudBackupState is CloudBackupState.Disabled) {
                add(
                    SecurityProblem.CloudBackupNotWorking.Disabled(
                        isAnyActivePersonaAffected = activePersonasNeedCloudBackup > 0,
                        hasManualBackup = cloudBackupState.lastManualBackupTime != null
                    )
                )
            } else if (cloudBackupState is CloudBackupState.Enabled && cloudBackupState.hasAnyErrors) {
                add(SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = activePersonasNeedCloudBackup > 0))
            }
        }.toSet()
    }
}
