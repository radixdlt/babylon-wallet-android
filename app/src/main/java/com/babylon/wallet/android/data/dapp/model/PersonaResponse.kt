package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OneTimePersonaDataResponseItem(
    override val requestType: String,
    @SerialName("accounts")
    val fields: List<PersonaDataField>
) : WalletResponseItem()

@Serializable
data class OngoingPersonaDataResponseItem(
    override val requestType: String,
    @SerialName("fields")
    val fields: List<PersonaDataField>
) : WalletResponseItem()

@Serializable
data class UsePersonaResponseItem(
    override val requestType: String,
    @SerialName("id")
    val id: String
) : WalletResponseItem()

enum class PersonaRequestType(val requestType: String) {
    ONE_TIME_PERSONA_DATA_READ("oneTimePersonaDataRead"),
    ONGOING_PERSONA_DATA_READ("ongoingPersonaDataRead"),
    USE_PERSONA_READ("usePersonaRead")
}
