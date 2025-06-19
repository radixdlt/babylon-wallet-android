package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.AccountAddress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.sargon.serializers.AccountAddressSerializer

@Serializable
data class DirectoryDefinition(
    @SerialName(value = "name")
    val name: String,
    @Serializable(with = AccountAddressSerializer::class)
    @SerialName(value = "address")
    val dAppDefinitionAddress: AccountAddress,
    @SerialName(value = "tags")
    val tags: List<String>,
    @SerialName(value = "category")
    val category: String?
)

@Serializable
data class DAppDirectory(
    @SerialName(value = "highlighted")
    val highlighted: List<DirectoryDefinition>? = emptyList(),
    @SerialName(value = "others")
    val others: List<DirectoryDefinition>? = emptyList()
) {

    val all: List<DirectoryDefinition> = highlighted.orEmpty() + others.orEmpty()

    fun findByAddress(address: AccountAddress): DirectoryDefinition? {
        return all.firstOrNull { it.dAppDefinitionAddress == address }
    }
}
