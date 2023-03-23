package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import rdx.works.profile.data.model.pernetwork.Network

@StringRes
fun Network.Persona.Field.Kind.toDisplayResource(): Int {
    return when (this) {
        Network.Persona.Field.Kind.FirstName -> R.string.first_name
        Network.Persona.Field.Kind.LastName -> R.string.last_name
        Network.Persona.Field.Kind.Email -> R.string.email
        Network.Persona.Field.Kind.PersonalIdentificationNumber -> R.string.personal_identification_number
        Network.Persona.Field.Kind.ZipCode -> R.string.zip_code
    }
}

@StringRes
fun Network.Persona.Field.Kind.toValidationError(): Int {
    return when (this) {
        Network.Persona.Field.Kind.FirstName -> R.string.first_name_empty
        Network.Persona.Field.Kind.LastName -> R.string.last_name_empty
        Network.Persona.Field.Kind.Email -> R.string.email_wrong
        Network.Persona.Field.Kind.PersonalIdentificationNumber -> R.string.personal_identification_number_empty
        Network.Persona.Field.Kind.ZipCode -> R.string.zip_code_empty
    }
}
