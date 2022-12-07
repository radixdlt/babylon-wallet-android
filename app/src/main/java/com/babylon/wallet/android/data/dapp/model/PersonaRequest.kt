package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("oneTimePersonaDataRead")
data class OneTimePersonaDataReadRequestItem(
    @SerialName("fields")
    val fields: List<String>
) : WalletRequestItem()

@Serializable
@SerialName("ongoingPersonaDataRead")
data class OngoingPersonaDataReadRequestItem(
    @SerialName("fields")
    val fields: List<PersonaDataField>
) : WalletRequestItem()

@Serializable
@SerialName("usePersonaRead")
data class UsePersonaReadRequestItem(
    @SerialName("id")
    val id: String
) : WalletRequestItem()
