package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activeEntitiesOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
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
        getProfileUseCase.flow.map { it.activeEntitiesOnCurrentNetwork },
        preferencesManager.getBackedUpFactorSourceIds()
    ) { entities, backedUpFactorSourceIds ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds)
        }
    }

    val shouldShowPersonaSecurityPrompt: Flow<Boolean>
        get() = combine(
            getProfileUseCase.flow.map { it.activePersonasOnCurrentNetwork },
            preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged()
        ) { personas, backedUpFactorSourceIds ->
            personas.any { persona ->
                val entity = mapToEntityWithSecurityPrompt(ProfileEntity.PersonaEntity(persona), backedUpFactorSourceIds)
                entity?.prompt == SecurityPromptType.NEEDS_BACKUP
            }
        }

    private suspend fun mapToEntityWithSecurityPrompt(
        entity: ProfileEntity,
        backedUpFactorSourceIds: Set<FactorSourceId.Hash>
    ): EntityWithSecurityPrompt? {
        val factorSourceId = entity.securityState.factorSourceId as? FactorSourceId.Hash ?: return null
        val factorSource = getProfileUseCase().factorSourceById(factorSourceId) as? FactorSource.Device ?: return null

        return if (!mnemonicRepository.mnemonicExist(factorSource.value.id.asGeneral())) {
            EntityWithSecurityPrompt(
                entity = entity,
                prompt = SecurityPromptType.NEEDS_RESTORE
            )
        } else if (!backedUpFactorSourceIds.contains(factorSourceId)) {
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
    val entity: ProfileEntity,
    val prompt: SecurityPromptType
)

enum class SecurityPromptType {
    NEEDS_BACKUP,
    NEEDS_RESTORE
}
