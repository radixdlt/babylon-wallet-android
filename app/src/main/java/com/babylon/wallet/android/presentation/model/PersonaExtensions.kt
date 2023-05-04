package com.babylon.wallet.android.presentation.model

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.utils.decodeUtf8
import com.babylon.wallet.android.utils.encodeUtf8
import rdx.works.profile.data.model.pernetwork.Network

@StringRes
fun Network.Persona.Field.ID.toDisplayResource(): Int {
    return when (this) {
        Network.Persona.Field.ID.GivenName -> R.string.given_name
        Network.Persona.Field.ID.FamilyName -> R.string.family_name
        Network.Persona.Field.ID.EmailAddress -> R.string.email
        Network.Persona.Field.ID.PhoneNumber -> R.string.phone
    }
}

fun List<Network.Persona.Field.ID>.encodeToString(): String {
    return joinToString(",") { it.name }.encodeUtf8()
}

fun String.decodePersonaDataKinds(): List<Network.Persona.Field.ID> {
    return decodeUtf8().split(",").filter { it.isNotEmpty() }.map { Network.Persona.Field.ID.valueOf(it) }
}

fun Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.toQuantifierUsedInRequest():
    MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier {
    return when (this) {
        Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly -> {
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
        }
        Network.AuthorizedDapp.AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.AtLeast -> {
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        }
    }
}

@StringRes
fun Network.Persona.Field.ID.toValidationError(): Int {
    return when (this) {
        Network.Persona.Field.ID.GivenName -> R.string.first_name_empty
        Network.Persona.Field.ID.FamilyName -> R.string.last_name_empty
        Network.Persona.Field.ID.EmailAddress -> R.string.email_wrong
        Network.Persona.Field.ID.PhoneNumber -> R.string.empty_phone
    }
}
