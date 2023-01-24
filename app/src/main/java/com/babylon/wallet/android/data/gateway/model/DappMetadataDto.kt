package com.babylon.wallet.android.data.gateway.model

import com.babylon.wallet.android.domain.model.DappMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DappMetadataDto(
    @SerialName(value = "id")
    val id: String,
    @SerialName(value = "dAppDefinitionAddress")
    val dAppDefinitionAddress: String
)

fun DappMetadataDto.toDomainModel(): DappMetadata {
    return DappMetadata(id, dAppDefinitionAddress)
}
