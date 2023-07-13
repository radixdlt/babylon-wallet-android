package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.decodeUtf8
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.isValidEmail
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber

@StringRes
fun PersonaData.PersonaDataField.Kind.toDisplayResource(): Int {
    return when (this) {
        PersonaData.PersonaDataField.Kind.Name -> R.string.authorizedDapps_personaDetails_name
        PersonaData.PersonaDataField.Kind.EmailAddress -> R.string.authorizedDapps_personaDetails_emailAddress
        PersonaData.PersonaDataField.Kind.PhoneNUmber -> R.string.authorizedDapps_personaDetails_phoneNumber
        else -> R.string.empty
    }
}

fun List<PersonaData.PersonaDataField.Kind>.encodeToString(): String {
    return joinToString(",") { it.name }.encodeUtf8()
}

fun String.decodePersonaDataKinds(): List<PersonaData.PersonaDataField.Kind> {
    return decodeUtf8().split(",").filter { it.isNotEmpty() }.map { PersonaData.PersonaDataField.Kind.valueOf(it) }
}

fun RequestedNumber.Quantifier.toQuantifierUsedInRequest():
    MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier {
    return when (this) {
        RequestedNumber.Quantifier.Exactly -> {
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
        }

        RequestedNumber.Quantifier.AtLeast -> {
            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.AtLeast
        }
    }
}

fun PersonaData.PersonaDataField.sortOrderInt(): Int {
    return kind.ordinal
}

fun PersonaData.PersonaDataField.isValid(): Boolean {
    return when (this) {
        is PersonaData.PersonaDataField.Email -> value.trim().isValidEmail()
        is PersonaData.PersonaDataField.Name -> given.trim().isNotEmpty() && family.isNotEmpty()
        is PersonaData.PersonaDataField.PhoneNumber -> value.trim().isNotEmpty()
        else -> true
    }
}

fun PersonaData.PersonaDataField.Kind.empty(): PersonaData.PersonaDataField {
    return when (this) {
        PersonaData.PersonaDataField.Kind.Name -> PersonaData.PersonaDataField.Name(
            PersonaData.PersonaDataField.Name.Variant.Western,
            "",
            ""
        )

        PersonaData.PersonaDataField.Kind.EmailAddress -> PersonaData.PersonaDataField.Email("")
        PersonaData.PersonaDataField.Kind.PhoneNUmber -> PersonaData.PersonaDataField.PhoneNumber("")
        else -> throw RuntimeException("Field $this not supported")
    }
}
