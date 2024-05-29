package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.allEntitiesOnCurrentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GetEntitiesWithSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository
) {

    operator fun invoke() = combine(
        getProfileUseCase.flow.map { it.allEntitiesOnCurrentNetwork },
        preferencesManager.getBackedUpFactorSourceIds()
    ) { entities, backedUpFactorSourceIds ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds)
        }
    }

    private suspend fun mapToEntityWithSecurityPrompt(
        entity: ProfileEntity,
        backedUpFactorSourceIds: Set<FactorSourceId.Hash>
    ): EntityWithSecurityPrompt? {
        val factorSourceId = entity.securityState.factorSourceId as? FactorSourceId.Hash ?: return null
        val factorSource = getProfileUseCase().factorSourceById(factorSourceId) as? FactorSource.Device ?: return null
        val prompts = mutableSetOf<SecurityPromptType>().apply {
            if (!mnemonicRepository.mnemonicExist(factorSource.value.id.asGeneral())) {
                add(SecurityPromptType.NEEDS_RECOVER)
            } else if (!backedUpFactorSourceIds.contains(factorSourceId)) {
                add(SecurityPromptType.NEEDS_BACKUP)
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
) {
    val prompt: SecurityPromptType
        get() = prompts.first()
}

enum class SecurityPromptType {
    NEEDS_BACKUP,
    NEEDS_RECOVER
}
