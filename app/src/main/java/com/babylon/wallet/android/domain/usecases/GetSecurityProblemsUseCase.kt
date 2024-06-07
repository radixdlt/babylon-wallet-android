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
        // personas that need cloud backup
        val personasNeedCloudBackup = entitiesWithSecurityPrompts
            .filter { it.entity is ProfileEntity.PersonaEntity }
            .filter {
                it.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM) ||
                    it.prompts.contains(SecurityPromptType.WALLET_NOT_RECOVERABLE) ||
                    it.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED)
            }
        val activePersonasNeedCloudBackup = personasNeedCloudBackup.count { it.entity.isNotHidden() }

        // entities that need to write down seed phrase
        val entitiesNeedingBackup = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.WRITE_DOWN_SEED_PHRASE) }
        val factorSourceIdsNeedBackup = entitiesNeedingBackup.map { it.entity.securityState.factorSourceId }.toSet()

        // entities that need recovery
        val entitiesNeedingRecovery = entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.RECOVERY_REQUIRED) }
        val factorSourceIdsNeedRecovery = entitiesNeedingRecovery.map { it.entity.securityState.factorSourceId }
        val isAnyActivePersonaNeedRecovery = entitiesNeedingRecovery.any {
            it.entity is ProfileEntity.PersonaEntity && it.entity.isNotHidden()
        }

        mutableSetOf<SecurityProblem>().apply {
            // entities that need cloud backup
            if (cloudBackupState is CloudBackupState.Disabled && cloudBackupState.isNotUpdated) {
                add(
                    SecurityProblem.CloudBackupNotWorking.Disabled(
                        isAnyActivePersonaAffected = activePersonasNeedCloudBackup > 0,
                        hasManualBackup = cloudBackupState.lastManualBackupTime != null
                    )
                )
            } else if (cloudBackupState is CloudBackupState.Enabled && cloudBackupState.hasAnyErrors) {
                add(SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = activePersonasNeedCloudBackup > 0))
            }

            // entities that need to write down seed phrase
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

            // entities that need recovery
            if (factorSourceIdsNeedRecovery.isNotEmpty()) {
                add(SecurityProblem.SeedPhraseNeedRecovery(isAnyActivePersonaAffected = isAnyActivePersonaNeedRecovery))
            }
        }.toSet()
    }
}
