package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.PersonaUiModel
import com.babylon.wallet.android.domain.model.toDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.utils.personaFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.deviceFactorSources
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

class GetPersonasUseCase @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val getProfileUseCase: GetProfileUseCase,
) {
    operator fun invoke(): Flow<List<PersonaUiModel>> {
        return getProfileUseCase.personasOnCurrentNetwork.map {
            val deviceFactorSources = getProfileUseCase.deviceFactorSources.first()
            val backedUpFactorSourcesIDs = preferencesManager.getBackedUpFactorSourceIds().first()
            it.map { persona ->
                val personaFactorSourceID = deviceFactorSources.firstOrNull { it.id == persona.personaFactorSourceId() }?.id
                val personaFactorSourceExistAndBackedUp =
                    personaFactorSourceID != null && backedUpFactorSourcesIDs.contains(personaFactorSourceID.value)
                persona.toDomainModel(personaFactorSourceExistAndBackedUp)
            }
        }
    }
}
