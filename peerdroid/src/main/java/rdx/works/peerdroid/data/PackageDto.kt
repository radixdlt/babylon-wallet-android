package rdx.works.peerdroid.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("packageType")
sealed class PackageDto {

    abstract val messageId: String

    @Serializable
    @SerialName("receiveMessageConfirmation")
    data class ReceiveMessageConfirmation(
        @SerialName("messageId")
        override val messageId: String
    ) : PackageDto()

    @Serializable
    @SerialName("receiveMessageError")
    data class ReceiveMessageError(
        @SerialName("messageId")
        override val messageId: String
    ) : PackageDto()

    @Serializable
    @SerialName("metaData")
    data class MetaData(
        @SerialName("messageId")
        override val messageId: String,
        @SerialName("chunkCount")
        val chunkCount: Int, // the number of chunks the message was split into
        @SerialName("hashOfMessage")
        val hashOfMessage: String, // hash of the original split message
        @SerialName("messageByteCount")
        val messageByteCount: Int
    ) : PackageDto()

    @Serializable
    @SerialName("chunk")
    data class Chunk(
        @SerialName("messageId")
        override val messageId: String,
        @SerialName("chunkIndex")
        val chunkIndex: Int,
        @SerialName("chunkData")
        val chunkData: String
    ) : PackageDto()
}
