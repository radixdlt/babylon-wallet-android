package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.model.fullName
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.fields

data class PersonaUiModel(
    val persona: Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0,
    val requiredPersonaFields: RequiredPersonaFields = RequiredPersonaFields(listOf())
) {
    fun missingFieldKinds(): PersistentList<PersonaDataField.Kind> {
        val requiredFieldKinds = requiredPersonaFields.fields.map { it.kind }
        return requiredFieldKinds.minus(
            persona.personaData.fields.map { it.value.kind }.toSet()
        ).sortedBy { it.ordinal }.toPersistentList()
    }

    fun personalInfoFormatted(): String {
        return buildString {
            val requiredFieldKinds = requiredPersonaFields.fields.map { it.kind }
            val fields = persona.personaData.fields.map { it.value }.filter { requiredFieldKinds.contains(it.kind) }
            val fullName = fields.filterIsInstance<PersonaDataField.Name>().firstOrNull()?.fullName
            val email = fields.filterIsInstance<PersonaDataField.Email>().firstOrNull()?.value
            val phone = fields.filterIsInstance<PersonaDataField.PhoneNumber>().firstOrNull()?.value
            append(
                listOfNotNull(fullName, email, phone).filter { it.isNotEmpty() }
                    .joinToString("\n")
            )
        }
    }
}

fun Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
