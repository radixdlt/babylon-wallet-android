package com.babylon.wallet.android.domain.usecases

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.asProfileEntity
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
        getProfileUseCase.flow.map { it.activeEntitiesOnCurrentNetwork },
        preferencesManager.getBackedUpFactorSourceIds()
    ) { entities, backedUpFactorSourceIds ->
        entities.mapNotNull { entity ->
            mapToEntityWithSecurityPrompt(entity, backedUpFactorSourceIds)
        }
    }

    private suspend fun mapToEntityWithSecurityPrompt(entity: ProfileEntity, backedUpFactorSourceIds: Set<String>): EntityWithSecurityPrompt? {
        val factorSourceId = entity.securityState.factorSourceId as? FactorSourceID.FromHash ?: return null
        val factorSource = getProfileUseCase.factorSourceById(factorSourceId) as? DeviceFactorSource ?: return null
        val prompts = mutableSetOf<SecurityPromptType>().apply {
            if (!mnemonicRepository.mnemonicExist(factorSource.id)) {
                add(SecurityPromptType.NEEDS_RESTORE)
            }
            if (!backedUpFactorSourceIds.contains(factorSourceId.body.value)) {
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
    NEEDS_RESTORE
}
