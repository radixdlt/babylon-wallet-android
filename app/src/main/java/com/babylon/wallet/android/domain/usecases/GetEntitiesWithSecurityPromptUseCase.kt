package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import rdx.works.core.domain.cloudbackup.CloudBackupDisabled
import rdx.works.core.domain.cloudbackup.CloudBackupServiceError
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.allEntitiesOnCurrentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetCloudBackupStateUseCase
import javax.inject.Inject

class GetEntitiesWithSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository,
    private val getCloudBackupStateUseCase: GetCloudBackupStateUseCase
) {

    operator fun invoke(): Flow<List<EntityWithSecurityPrompt>> = combine(
        getProfileUseCase.flow.map { it.allEntitiesOnCurrentNetwork },
        preferencesManager.getBackedUpFactorSourceIds(),
        getCloudBackupStateUseCase()
    ) { entities, backedUpFactorSourceIds, cloudBackupState ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds, cloudBackupState)
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun mapToEntityWithSecurityPrompt(
        entity: ProfileEntity,
        backedUpFactorSourceIds: Set<FactorSourceId.Hash>,
        cloudBackupState: CloudBackupState
    ): EntityWithSecurityPrompt? {
        val factorSourceId = entity.securityState.factorSourceId as? FactorSourceId.Hash ?: return null
        val factorSource = getProfileUseCase().factorSourceById(factorSourceId) as? FactorSource.Device ?: return null

        val prompts = mutableSetOf<SecurityPromptType>().apply {
            cloudBackupState.backupWarning?.let { backupWarning ->
                when (backupWarning) {
                    is CloudBackupDisabled -> {
                        if (backupWarning.hasUpdatedManualBackup) {
                            add(SecurityPromptType.CONFIGURATION_BACKUP_NOT_UPDATED)
                        } else {
                            add(SecurityPromptType.WALLET_NOT_RECOVERABLE)
                        }
                    }
                    CloudBackupServiceError -> add(SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM)
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
