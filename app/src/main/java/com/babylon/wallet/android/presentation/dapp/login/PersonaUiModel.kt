package com.babylon.wallet.android.presentation.dapp.login

import rdx.works.profile.data.model.pernetwork.OnNetwork

data class PersonaUiModel(
    val persona: OnNetwork.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val sharedAccountNumber: Int = 0
)

fun OnNetwork.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
