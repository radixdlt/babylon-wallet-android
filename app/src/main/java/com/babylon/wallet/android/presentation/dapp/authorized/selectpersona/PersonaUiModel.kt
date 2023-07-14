package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.toRequestedFieldKinds
import com.babylon.wallet.android.presentation.model.fullName
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData

data class PersonaUiModel(
    val persona: Network.Persona,
    val selected: Boolean = false,
    val pinned: Boolean = false,
    val lastUsedOn: String? = null,
    val lastUsedOnTimestamp: Long = 0,
    val requestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem? = null
) {
    fun missingFieldKinds(): PersistentList<PersonaData.PersonaDataField.Kind> {
        val requiredFieldKinds = requestItem?.toRequestedFieldKinds().orEmpty()
        return requiredFieldKinds.minus(persona.personaData.allFields.map { it.value.kind }.toSet()).sortedBy { it.ordinal }
            .toPersistentList()
    }

    fun personalInfoFormatted(): String {
        return buildString {
            val requiredFieldKinds = requestItem?.toRequestedFieldKinds().orEmpty()
            val fields = persona.personaData.allFields.map { it.value }.filter { requiredFieldKinds.contains(it.kind) }
            val fullName = fields.filterIsInstance<PersonaData.PersonaDataField.Name>().firstOrNull()?.fullName
            val email = fields.filterIsInstance<PersonaData.PersonaDataField.Email>().firstOrNull()?.value
            val phone = fields.filterIsInstance<PersonaData.PersonaDataField.PhoneNumber>().firstOrNull()?.value
            append(
                listOfNotNull(fullName, email, phone).filter { it.isNotEmpty() }
                    .joinToString("\n")
            )
        }
    }
}

fun Network.Persona.toUiModel(): PersonaUiModel {
    return PersonaUiModel(this)
}
