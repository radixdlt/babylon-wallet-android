package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

data class PersonaUiModel(
    val persona: Network.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0,
    val requiredFieldIDs: List<PersonaDataEntryID> = emptyList()
) {
    fun missingFieldKinds(): ImmutableList<PersonaDataEntryID> {
        // TODO persona data
//        return requiredFieldIDs.minus(persona.fields.map { it.id }.toSet()).sortedBy { it.ordinal }.toPersistentList()
        return persistentListOf()
    }

    fun personalInfoFormatted(): String {
        return buildString {
            // TODO persona data
//            val fields = persona.fields.filter { requiredFieldIDs.contains(it.id) }
//            val givenName = fields.firstOrNull { it.id == Network.Persona.Field.ID.GivenName }?.value
//            val familyName = fields.firstOrNull { it.id == Network.Persona.Field.ID.FamilyName }?.value
//            val email = fields.firstOrNull { it.id == Network.Persona.Field.ID.EmailAddress }?.value
//            val phone = fields.firstOrNull { it.id == Network.Persona.Field.ID.PhoneNumber }?.value
//            append(
//                listOfNotNull(listOfNotNull(givenName, familyName).joinToString(separator = " "), email, phone).filter { it.isNotEmpty() }
//                    .joinToString("\n")
//            )
        }
    }
}

fun Network.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
