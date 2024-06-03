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
            val activePersonasNeedBackup = personasNeedBackup.count { it.entity.isNotHidden() }

            if (factorSourceIdsNeedBackup.isNotEmpty()) {
                add(
                    SecurityProblem.EntitiesNotRecoverable(
                        accountsNeedBackup = accountsNeedBackup.count { it.entity.isNotHidden() },
                        personasNeedBackup = activePersonasNeedBackup,
                        hiddenAccountsNeedBackup = accountsNeedBackup.count { it.entity.isHidden() },
                        hiddenPersonasNeedBackup = personasNeedBackup.count { it.entity.isHidden() }
                    )
                )
            }

            if (cloudBackupState is CloudBackupState.Disabled) {
                add(
                    SecurityProblem.CloudBackupNotWorking.Disabled(
                        isAnyActivePersonaAffected = activePersonasNeedBackup > 0,
                        hasManualBackup = cloudBackupState.lastManualBackupTime != null
                    )
                )
            } else if (cloudBackupState is CloudBackupState.Enabled && cloudBackupState.hasAnyErrors) {
                add(SecurityProblem.CloudBackupNotWorking.ServiceError(isAnyActivePersonaAffected = activePersonasNeedBackup > 0))
            }
        }.toSet()
    }
}
