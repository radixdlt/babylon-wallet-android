package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.domain.model.DappWithMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DappMetadataDto(
    @SerialName(value = "dAppDefinitionAddress")
    val dAppDefinitionAddress: String
)

fun DappMetadataDto.toDomainModel(): DappWithMetadata {
    return DappWithMetadata(dAppDefinitionAddress)
}
