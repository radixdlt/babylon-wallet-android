package com.babylon.wallet.android.domain.model

import rdx.works.profile.data.model.pernetwork.Network

data class PersonaUiModel(
    val address: String,
    val displayName: String,
    val mnemonicBackedUp: Boolean = false,
)

fun Network.Persona.toDomainModel(mnemonicBackedUp: Boolean): PersonaUiModel {
    return PersonaUiModel(
        address = this.address,
        displayName = displayName,
        mnemonicBackedUp = mnemonicBackedUp
    )
}
