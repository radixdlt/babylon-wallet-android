package com.babylon.wallet.android.presentation.dapp.selectpersona

import rdx.works.profile.data.model.pernetwork.Network

data class PersonaUiModel(
    val persona: Network.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0
)

fun Network.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
