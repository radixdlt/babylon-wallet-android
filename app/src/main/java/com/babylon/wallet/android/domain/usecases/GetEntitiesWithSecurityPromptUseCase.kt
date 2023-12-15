package com.babylon.wallet.android.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.entitiesOnCurrentNetwork
import rdx.works.profile.domain.factorSourceById
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

class GetEntitiesWithSecurityPromptUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val mnemonicRepository: MnemonicRepository
) {

    operator fun invoke() = combine(
        getProfileUseCase.entitiesOnCurrentNetwork,
        preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
    ) { entities, backedUpFactorSourceIds ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds)
        }
    }

    val shouldShowPersonaSecurityPrompt: Flow<Boolean>
        get() = combine(
            getProfileUseCase.personasOnCurrentNetwork,
            preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
        ) { personas, backedUpFactorSourceIds ->
            personas.any { persona ->
                val entity = mapToEntityWithSecurityPrompt(persona, backedUpFactorSourceIds)
                entity?.prompt == SecurityPromptType.NEEDS_BACKUP
            }
        }

    private suspend fun mapToEntityWithSecurityPrompt(entity: Entity, backedUpFactorSourceIds: Set<String>): EntityWithSecurityPrompt? {
        val factorSourceId = entity.factorSourceId as? FactorSourceID.FromHash ?: return null
        val factorSource = getProfileUseCase.factorSourceById(factorSourceId) as? DeviceFactorSource ?: return null

        return if (!mnemonicRepository.mnemonicExist(factorSource.id)) {
            EntityWithSecurityPrompt(
                entity = entity,
                prompt = SecurityPromptType.NEEDS_RESTORE
            )
        } else if (!backedUpFactorSourceIds.contains(factorSourceId.body.value)) {
            EntityWithSecurityPrompt(
                entity = entity,
                prompt = SecurityPromptType.NEEDS_BACKUP
            )
        } else {
            null
        }
    }
}

data class EntityWithSecurityPrompt(
    val entity: Entity,
    val prompt: SecurityPromptType
)

enum class SecurityPromptType {
    NEEDS_BACKUP,
    NEEDS_RESTORE
}
