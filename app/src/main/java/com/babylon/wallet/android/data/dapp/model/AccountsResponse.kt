package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Serializable
data class OneTimeAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OneTimeAccountsRequestResponseItem()

@Serializable
data class OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OneTimeAccountsRequestResponseItem()

@Serializable
data class OngoingAccountsWithProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountWithProofOfOwnership>
) : OngoingAccountsRequestResponseItem()

@Serializable
data class OngoingAccountsWithoutProofOfOwnershipRequestResponseItem(
    @SerialName("accounts")
    val accounts: List<AccountDto>
) : OngoingAccountsRequestResponseItem()

@Serializable(with = OngoingAccountsRequestResponseItemSerializer::class)
@Suppress("UnnecessaryAbstractClass")
sealed class OngoingAccountsRequestResponseItem

@Serializable(with = OneTimeAccountsRequestResponseItemSerializer::class)
@Suppress("UnnecessaryAbstractClass")
sealed class OneTimeAccountsRequestResponseItem

object OneTimeAccountsRequestResponseItemSerializer :
    JsonContentPolymorphicSerializer<OneTimeAccountsRequestResponseItem>(OneTimeAccountsRequestResponseItem::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out OneTimeAccountsRequestResponseItem> {
        val isResponseWithProof = try {
            element.jsonObject["accounts"]?.jsonArray?.get(0)?.jsonObject?.get("account") != null
        } catch (e: Exception) {
            false
        }
        return if (isResponseWithProof) {
            OneTimeAccountsWithProofOfOwnershipRequestResponseItem.serializer()
        } else {
            OneTimeAccountsWithoutProofOfOwnershipRequestResponseItem.serializer()
        }
    }
}

object OngoingAccountsRequestResponseItemSerializer :
    JsonContentPolymorphicSerializer<OngoingAccountsRequestResponseItem>(OngoingAccountsRequestResponseItem::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out OngoingAccountsRequestResponseItem> {
        val isResponseWithProof = try {
            element.jsonObject["accounts"]?.jsonArray?.get(0)?.jsonObject?.get("account") != null
        } catch (e: Exception) {
            false
        }
        return if (isResponseWithProof) {
            OngoingAccountsWithProofOfOwnershipRequestResponseItem.serializer()
        } else {
            OngoingAccountsWithoutProofOfOwnershipRequestResponseItem.serializer()
        }
    }
}
