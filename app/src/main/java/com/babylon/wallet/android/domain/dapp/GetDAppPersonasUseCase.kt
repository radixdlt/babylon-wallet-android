package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.PersonaEntityUiState
import com.babylon.wallet.android.domain.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDAppPersonasUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend fun getDAppPersonas(): List<PersonaEntityUiState> = profileRepository.getPersonas().map { personaEntity ->
        PersonaEntityUiState(personaEntity, false)
    }
}