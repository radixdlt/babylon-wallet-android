package com.babylon.wallet.android.presentation.dapp.selectpersona

import rdx.works.profile.data.model.pernetwork.OnNetwork

data class PersonaUiModel(
    val persona: OnNetwork.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0
)

fun OnNetwork.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
