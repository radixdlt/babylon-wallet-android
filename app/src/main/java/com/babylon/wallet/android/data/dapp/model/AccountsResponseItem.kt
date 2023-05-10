@file:OptIn(ExperimentalSerializationApi::class)

package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class AccountDto(
    @SerialName("address") val address: String,
    @SerialName("label") val label: String,
    @SerialName("appearanceId") val appearanceId: Int,
)

@Serializable
data class AccountWithProofOfOwnership(
    @SerialName("account") val accountDto: AccountDto,
    @SerialName("proof") val proof: ProofDto
)

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class OneTimeAccountsRequestResponseItem

@Serializable
@SerialName("oneTimeAccountsWithProofOfOwnership")
data class OneTimeAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("challenge")
    val challenge: String,
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OneTimeAccountsRequestResponseItem()

@Serializable
@SerialName("oneTimeAccountsWithoutProofOfOwnership")
data class OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OneTimeAccountsRequestResponseItem()

@Serializable
@JsonClassDiscriminator("discriminator")
sealed class OngoingAccountsRequestResponseItem

@Serializable
@SerialName("ongoingAccountsWithProofOfOwnership")
data class OngoingAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OngoingAccountsRequestResponseItem()

@Serializable
@SerialName("ongoingAccountsWithoutProofOfOwnership")
data class OngoingAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OngoingAccountsRequestResponseItem()

fun List<AccountItemUiModel>.toDataModel() = map { accountResources ->
    AccountDto(
        address = accountResources.address,
        label = accountResources.displayName.orEmpty(),
        appearanceId = accountResources.appearanceID
    )
}
