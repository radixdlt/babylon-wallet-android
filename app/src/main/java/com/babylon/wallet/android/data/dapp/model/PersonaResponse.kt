package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimePersonaDataRequestResponseItem(
    @SerialName("accounts")
    val fields: List<PersonaDataField>
)

@Serializable
data class OngoingPersonaDataRequestResponseItem(
    @SerialName("fields")
    val fields: List<PersonaDataField>
)
