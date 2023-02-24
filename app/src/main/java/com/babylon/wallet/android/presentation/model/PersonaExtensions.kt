package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import rdx.works.profile.data.model.pernetwork.OnNetwork

@StringRes
fun OnNetwork.Persona.Field.Kind.toDisplayResource(): Int {
    return when (this) {
        OnNetwork.Persona.Field.Kind.FirstName -> R.string.first_name
        OnNetwork.Persona.Field.Kind.LastName -> R.string.last_name
        OnNetwork.Persona.Field.Kind.Email -> R.string.email
        OnNetwork.Persona.Field.Kind.PersonalIdentificationNumber -> R.string.personal_identification_number
        OnNetwork.Persona.Field.Kind.ZipCode -> R.string.zip_code
    }
}

@StringRes
fun OnNetwork.Persona.Field.Kind.toValidationError(): Int {
    return when (this) {
        OnNetwork.Persona.Field.Kind.FirstName -> R.string.first_name_empty
        OnNetwork.Persona.Field.Kind.LastName -> R.string.last_name_empty
        OnNetwork.Persona.Field.Kind.Email -> R.string.email_wrong
        OnNetwork.Persona.Field.Kind.PersonalIdentificationNumber -> R.string.personal_identification_number_empty
        OnNetwork.Persona.Field.Kind.ZipCode -> R.string.zip_code_empty
    }
}
