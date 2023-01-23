package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonaDto(
    @SerialName("identityAddress") val identityAddress: String,
    @SerialName("label") val label: String,
)

@Serializable
data class PersonaDataField(
    @SerialName("field") val field: String,
    @SerialName("value") val value: String,
)