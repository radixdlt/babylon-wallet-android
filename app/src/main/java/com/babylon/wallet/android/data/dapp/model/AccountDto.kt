package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.AccountSlim
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    @SerialName("address")
    val address: String,
    @SerialName("label")
    val label: String,
    @SerialName("appearanceId")
    val appearanceId: Int
)

@Serializable
data class AccountWithProofOfOwnership(
    @SerialName("account")
    val accountDto: AccountDto,
    @SerialName("challenge")
    val challenge: String,
    @SerialName("signature")
    val signature: String
)

@Serializable
data class PersonaDataField(
    @SerialName("field")
    val field: String,
    @SerialName("value")
    val value: String
)

fun List<AccountSlim>.toDataModel() = map { accountResources ->
    AccountDto(
        address = accountResources.address,
        label = accountResources.displayName.orEmpty(),
        appearanceId = accountResources.appearanceID
    )
}
