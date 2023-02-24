package com.babylon.wallet.android.fakes

import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.domain.UpdatePersonaUseCase

class UpdatePersonaUseCaseFake : UpdatePersonaUseCase {

    override suspend fun invoke(updatedPersona: OnNetwork.Persona) {
    }
}
