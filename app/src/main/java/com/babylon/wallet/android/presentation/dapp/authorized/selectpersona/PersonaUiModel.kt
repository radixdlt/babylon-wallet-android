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
    /**
     * The kinds of data the request needs and this persona does not have enough of: a name, or as
     * many email addresses and phone numbers as asked for. Sharing goes on only without any, or it
     * would fail half way (and it used to close the app).
     */
    fun missingFieldKinds(): PersistentList<PersonaDataField.Kind> {
        val data = persona.personaData
        return requiredPersonaFields.fields
            .filter { required ->
                val held = when (required.kind) {
                    PersonaDataField.Kind.Name -> if (data.name != null) Int.MAX_VALUE else 0
                    PersonaDataField.Kind.EmailAddress -> data.emailAddresses.collection.size
                    PersonaDataField.Kind.PhoneNumber -> data.phoneNumbers.collection.size
                    else -> 0
                }
                held < required.numberOfValues.quantity.coerceAtLeast(1)
            }
            .map { it.kind }
            .distinct()
            .sortedBy { it.ordinal }
            .toPersistentList()
    }

    /** Whether this persona has all the data the request needs. */
    val hasRequiredData: Boolean
        get() = missingFieldKinds().isEmpty()

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
