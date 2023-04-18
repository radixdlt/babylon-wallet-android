package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimePersonaDataRequestResponseItem(
    @SerialName("fields")
    val fields: List<PersonaData>
)

@Serializable
data class OngoingPersonaDataRequestResponseItem(
    @SerialName("fields")
    val fields: List<PersonaData>
)
