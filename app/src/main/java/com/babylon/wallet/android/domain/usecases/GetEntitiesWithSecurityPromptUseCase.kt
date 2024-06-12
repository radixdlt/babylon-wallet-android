package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import rdx.works.core.domain.cloudbackup.BackupState
import rdx.works.core.domain.cloudbackup.BackupWarning
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.allEntitiesOnCurrentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

class GetEntitiesWithSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository,
    private val getBackupStateUseCase: GetBackupStateUseCase
) {

    operator fun invoke(): Flow<List<EntityWithSecurityPrompt>> = combine(
        getProfileUseCase.flow.map { it.allEntitiesOnCurrentNetwork },
        preferencesManager.getBackedUpFactorSourceIds(),
        getBackupStateUseCase()
    ) { entities, backedUpFactorSourceIds, cloudBackupState ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds, cloudBackupState)
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun mapToEntityWithSecurityPrompt(
        entity: ProfileEntity,
        backedUpFactorSourceIds: Set<FactorSourceId.Hash>,
        backupState: BackupState
    ): EntityWithSecurityPrompt? {
        val factorSourceId = entity.securityState.factorSourceId as? FactorSourceId.Hash ?: return null
        val factorSource = getProfileUseCase().factorSourceById(factorSourceId) as? FactorSource.Device ?: return null

        val prompts = mutableSetOf<SecurityPromptType>().apply {
            backupState.backupWarning?.let { backupWarning ->
                when (backupWarning) {
                    BackupWarning.CLOUD_BACKUP_SERVICE_ERROR -> add(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM)
                    BackupWarning.CLOUD_BACKUP_DISABLED_WITH_NO_MANUAL_BACKUP -> add(SecurityPromptType.WALLET_NOT_RECOVERABLE)
                    BackupWarning.CLOUD_BACKUP_DISABLED_WITH_OUTDATED_MANUAL_BACKUP -> add(
                        SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED
                    )
                }
            }

            if (!mnemonicRepository.mnemonicExist(factorSource.value.id.asGeneral())) {
                add(SecurityPromptType.RECOVERY_REQUIRED)
            } else if (!backedUpFactorSourceIds.contains(factorSourceId)) {
                add(SecurityPromptType.WRITE_DOWN_SEED_PHRASE)
            }
        }.toSet()

        return if (prompts.isNotEmpty()) {
            EntityWithSecurityPrompt(
                entity = entity,
                prompts = prompts
            )
        } else {
            null
        }
    }
}

data class EntityWithSecurityPrompt(
    val entity: ProfileEntity,
    val prompts: Set<SecurityPromptType>
)

enum class SecurityPromptType {
    WRITE_DOWN_SEED_PHRASE,
    RECOVERY_REQUIRED,
    CONFIGURATION_BACKUP_PROBLEM,
    WALLET_NOT_RECOVERABLE,
    CONFIGURATION_BACKUP_NOT_UPDATED
}
