package rdx.works.peerdroid.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.domain.BasePackage

@Serializable
data class PackageMessageDto(
    @SerialName("packageType")
    val packageType: String,
    @SerialName("messageId")
    val messageId: String,
    @SerialName("chunkCount")
    val chunkCount: Int? = null, // metadata, the number of chunks the message was split into
    @SerialName("hashOfMessage")
    val hashOfMessage: String? = null, // metadata, hash of the original split message
    @SerialName("messageByteCount")
    val messageByteCount: Int? = null, // metadata
    @SerialName("chunkIndex")
    val chunkIndex: Int? = null, // message chunk
    @SerialName("chunkData")
    val chunkData: String? = null, // message chunk
    @SerialName("error")
    val error: String? = null
) {

    enum class PackageType(val type: String) {
        MESSAGE_CONFIRMATION("receiveMessageConfirmation"),
        MESSAGE_ERROR("receiveMessageError"),
        METADATA("metaData"),
        CHUNK("chunk");

        companion object {
            fun from(type: String): PackageType = requireNotNull(
                values().find { packageType -> packageType.type == type }
            ) {
                "No package type with type $type"
            }
        }
    }

    companion object {
        fun PackageMessageDto.toMetadata() = BasePackage.MetadataPackage(
            messageId = messageId,
            chunkCount = chunkCount ?: throw IllegalArgumentException("chunkCount is null"),
            hashOfMessage = hashOfMessage?.decodeHex()?.toByteArray()
                ?: throw IllegalArgumentException("hashOfMessage is null"),
            messageByteCount = messageByteCount ?: throw IllegalArgumentException("messageByteCount is null")
        )

        fun PackageMessageDto.toChunk() = BasePackage.ChunkPackage(
            messageId = messageId,
            chunkIndex = chunkIndex ?: throw IllegalArgumentException("chunkIndex is null"),
            chunkData = chunkData ?: throw IllegalArgumentException("chunkData is null")
        )
    }
}
