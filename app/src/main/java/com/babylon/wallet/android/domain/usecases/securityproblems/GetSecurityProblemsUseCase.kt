package com.babylon.wallet.android.domain.usecases.securityproblems

import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.domain.model.toDomainModel
import com.radixdlt.sargon.AddressesOfEntitiesInBadState
import com.radixdlt.sargon.BackupResult
import com.radixdlt.sargon.CheckSecurityProblemsInput
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.isNotHidden
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

class GetSecurityProblemsUseCase @Inject constructor(
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getBackupStateUseCase: GetBackupStateUseCase,
    private val sargonOsManager: SargonOsManager
) {

    operator fun invoke(): Flow<Set<SecurityProblem>> = combine(
        getEntitiesWithSecurityPromptUseCase(),
        getBackupStateUseCase()
    ) { entitiesWithSecurityPrompts, backupState ->

        // active personas that need cloud backup
        val activePersonasNeedCloudBackup = entitiesWithSecurityPrompts
            .count { entityWithSecurityPrompt ->
                entityWithSecurityPrompt.entity.isNotHidden() &&
                    entityWithSecurityPrompt.entity is ProfileEntity.PersonaEntity &&
                    (
                        entityWithSecurityPrompt.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM) ||
                            entityWithSecurityPrompt.prompts.contains(SecurityPromptType.WALLET_NOT_RECOVERABLE) ||
                            entityWithSecurityPrompt.prompts.contains(SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED)
                        )
            }

        sargonOsManager.sargonOs.checkSecurityProblems(
            input = CheckSecurityProblemsInput(
                isCloudProfileSyncEnabled = backupState.isCloudBackupEnabled,
                unrecoverableEntities = findUnrecoverableEntities(entitiesWithSecurityPrompts),
                withoutControlEntities = findWithoutControlEntities(entitiesWithSecurityPrompts),
                lastCloudBackup = BackupResult(
                    saveIdentifier = "string",
                    isCurrent = backupState.isCloudBackupSynced,
                    isFailed = backupState is BackupState.CloudBackupEnabled && backupState.hasAnyErrors
                ),
                lastManualBackup = BackupResult(
                    saveIdentifier = "string",
                    isCurrent = backupState.isManualBackupSynced,
                    isFailed = false,
                )
            )
        ).map { sargonSecurityProblem ->
            sargonSecurityProblem.toDomainModel(
                isAnyActivePersonaAffected = activePersonasNeedCloudBackup > 0,
                hasManualBackup = backupState.lastManualBackupTime != null
            )
        }.toSet()
    }

    private fun findUnrecoverableEntities(entitiesWithSecurityPrompts: List<EntityWithSecurityPrompt>): AddressesOfEntitiesInBadState {
        // entities that need to write down seed phrase
        val entitiesNeedBackup = entitiesWithSecurityPrompts
            .filter { entityWithSecurityPrompt ->
                entityWithSecurityPrompt.prompts.contains(SecurityPromptType.WRITE_DOWN_SEED_PHRASE)
            }
        val factorSourceIdsNeedBackup = entitiesNeedBackup.map { entityWithSecurityPrompt ->
            entityWithSecurityPrompt.entity.securityState.factorSourceId
        }.toSet()
        // not hidden and hidden accounts that need to write down seed phrase
        val (activeAccountAddressesNeedBackup, hiddenAccountAddressesNeedBackup) = entitiesNeedBackup
            .filter { entityWithSecurityPrompt ->
                entityWithSecurityPrompt.entity is ProfileEntity.AccountEntity &&
                    factorSourceIdsNeedBackup.contains(entityWithSecurityPrompt.entity.securityState.factorSourceId)
            }
            .map { entityWithSecurityPrompt -> entityWithSecurityPrompt.entity as ProfileEntity.AccountEntity }
            .partition { accountEntity -> accountEntity.isNotHidden() }
            .let { partitioned ->
                partitioned.first.map { it.accountAddress } to partitioned.second.map { it.accountAddress }
            }
        // not hidden and hidden personas that need to write down seed phrase
        val (activePersonaAddressesNeedBackup, hiddenPersonaAddressesNeedBackup) = entitiesNeedBackup
            .filter { entityWithSecurityPrompt ->
                entityWithSecurityPrompt.entity is ProfileEntity.PersonaEntity &&
                    factorSourceIdsNeedBackup.contains(entityWithSecurityPrompt.entity.securityState.factorSourceId)
            }
            .map { entityWithSecurityPrompt -> entityWithSecurityPrompt.entity as ProfileEntity.PersonaEntity }
            .partition { personaEntity -> personaEntity.isNotHidden() }
            .let { partitioned ->
                partitioned.first.map { it.identityAddress } to partitioned.second.map { it.identityAddress }
            }

        return AddressesOfEntitiesInBadState(
            accounts = activeAccountAddressesNeedBackup,
            hiddenAccounts = hiddenAccountAddressesNeedBackup,
            personas = activePersonaAddressesNeedBackup,
            hiddenPersonas = hiddenPersonaAddressesNeedBackup
        )
    }

    private fun findWithoutControlEntities(entitiesWithSecurityPrompts: List<EntityWithSecurityPrompt>): AddressesOfEntitiesInBadState {
        // entities that need recovery
        val entitiesNeedRecovery =
            entitiesWithSecurityPrompts.filter { it.prompts.contains(SecurityPromptType.RECOVERY_REQUIRED) }
        val (activeEntitiesNeedRecovery, hiddenEntitiesNeedRecovery) = entitiesNeedRecovery
            .partition { it.entity.isNotHidden() }
        val activeAccountAddressesNeedRecovery = activeEntitiesNeedRecovery
            .mapNotNull { (it.entity as? ProfileEntity.AccountEntity)?.accountAddress }
        val activePersonaAddressesNeedRecovery = activeEntitiesNeedRecovery
            .mapNotNull { (it.entity as? ProfileEntity.PersonaEntity)?.identityAddress }
        val hiddenAccountAddressesNeedRecovery = hiddenEntitiesNeedRecovery
            .mapNotNull { (it.entity as? ProfileEntity.AccountEntity)?.accountAddress }
        val hiddenPersonaAddressesNeedRecovery = hiddenEntitiesNeedRecovery
            .mapNotNull { (it.entity as? ProfileEntity.PersonaEntity)?.identityAddress }

        return AddressesOfEntitiesInBadState(
            accounts = activeAccountAddressesNeedRecovery,
            hiddenAccounts = hiddenAccountAddressesNeedRecovery,
            personas = activePersonaAddressesNeedRecovery,
            hiddenPersonas = hiddenPersonaAddressesNeedRecovery
        )
    }
}
