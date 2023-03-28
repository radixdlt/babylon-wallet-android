package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.utils.decodeUtf8
import com.babylon.wallet.android.utils.encodeUtf8
import rdx.works.profile.data.model.pernetwork.Network

@StringRes
fun Network.Persona.Field.Kind.toDisplayResource(): Int {
    return when (this) {
        Network.Persona.Field.Kind.GivenName -> R.string.given_name
        Network.Persona.Field.Kind.FamilyName -> R.string.family_name
        Network.Persona.Field.Kind.EmailAddress -> R.string.email
        Network.Persona.Field.Kind.PhoneNumber -> R.string.phone
    }
}

fun Network.Persona.personalInfoFormatted(): String {
    return buildString {
        val givenName = fields.firstOrNull { it.kind == Network.Persona.Field.Kind.GivenName }?.value
        val familyName = fields.firstOrNull { it.kind == Network.Persona.Field.Kind.FamilyName }?.value
        val email = fields.firstOrNull { it.kind == Network.Persona.Field.Kind.EmailAddress }?.value
        val phone = fields.firstOrNull { it.kind == Network.Persona.Field.Kind.PhoneNumber }?.value
        append(
            listOfNotNull(listOfNotNull(givenName, familyName).joinToString(separator = " "), email, phone).filter { it.isNotEmpty() }
                .joinToString("\n")
        )
    }
}

fun List<Network.Persona.Field.Kind>.encodeToString(): String {
    return joinToString(",") { it.name }.encodeUtf8()
}

fun String.decodePersonaDataKinds(): List<Network.Persona.Field.Kind> {
    val split = decodeUtf8().split(",")
    return split.filter { it.isNotEmpty() }.map { Network.Persona.Field.Kind.valueOf(it) }
}

@StringRes
fun Network.Persona.Field.Kind.toValidationError(): Int {
    return when (this) {
        Network.Persona.Field.Kind.GivenName -> R.string.first_name_empty
        Network.Persona.Field.Kind.FamilyName -> R.string.last_name_empty
        Network.Persona.Field.Kind.EmailAddress -> R.string.email_wrong
        Network.Persona.Field.Kind.PhoneNumber -> R.string.empty_phone
    }
}
