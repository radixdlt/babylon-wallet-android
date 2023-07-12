package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.profile.data.model.pernetwork.RequestedNumber

//TODO persona data
//@StringRes
//fun Network.Persona.Field.ID.toDisplayResource(): Int {
//    return when (this) {
//        Network.Persona.Field.ID.GivenName -> R.string.authorizedDapps_personaDetails_firstName
//        Network.Persona.Field.ID.FamilyName -> R.string.authorizedDapps_personaDetails_lastName
//        Network.Persona.Field.ID.EmailAddress -> R.string.authorizedDapps_personaDetails_emailAddress
//        Network.Persona.Field.ID.PhoneNumber -> R.string.authorizedDapps_personaDetails_phoneNumber
//    }
//}
//
//fun List<Network.Persona.Field.ID>.encodeToString(): String {
//    return joinToString(",") { it.name }.encodeUtf8()
//}
//
//fun String.decodePersonaDataKinds(): List<Network.Persona.Field.ID> {
//    return decodeUtf8().split(",").filter { it.isNotEmpty() }.map { Network.Persona.Field.ID.valueOf(it) }
//}

fun RequestedNumber.Quantifier.toQuantifierUsedInRequest():
    MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier {
    return when (this) {
        RequestedNumber.Quantifier.Exactly -> {
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.Exactly
        }
        RequestedNumber.Quantifier.AtLeast -> {
            MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier.AtLeast
        }
    }
}
