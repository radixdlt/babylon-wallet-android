package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.profile.data.model.pernetwork.Network

data class PersonaUiModel(
    val persona: Network.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0,
    val requiredFieldKinds: List<Network.Persona.Field.Kind> = emptyList()
) {
    fun missingFieldKinds(): ImmutableList<Network.Persona.Field.Kind> {
        return requiredFieldKinds.minus(persona.fields.map { it.kind }.toSet()).sortedBy { it.ordinal }.toPersistentList()
    }
}

fun Network.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
