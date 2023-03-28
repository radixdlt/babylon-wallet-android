package rdx.works.peerdroid.domain

import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PackageMessageDto

sealed interface BasePackage {
    val messageId: String

    data class MetadataPackage(
        override val messageId: String,
        val chunkCount: Int,
        val hashOfMessage: ByteArray,
        val messageByteCount: Int
    ) : BasePackage {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MetadataPackage

            if (messageId != other.messageId) return false
            if (chunkCount != other.chunkCount) return false
            if (!hashOfMessage.contentEquals(other.hashOfMessage)) return false
            if (messageByteCount != other.messageByteCount) return false

            return true
        }

        override fun hashCode(): Int {
            var result = messageId.hashCode()
            result = 31 * result + chunkCount
            result = 31 * result + hashOfMessage.contentHashCode()
            result = 31 * result + messageByteCount
            return result
        }

        companion object {
            fun MetadataPackage.toPackageMessageDto() = PackageMessageDto(
                packageType = PackageMessageDto.PackageType.METADATA.type,
                messageId = messageId,
                hashOfMessage = hashOfMessage.toHexString(),
                messageByteCount = messageByteCount,
                chunkCount = chunkCount
            )
        }
    }

    data class ChunkPackage(
        override val messageId: String,
        val chunkIndex: Int,
        val chunkData: String
    ) : BasePackage {

        companion object {
            fun ChunkPackage.toPackageMessageDto() = PackageMessageDto(
                packageType = PackageMessageDto.PackageType.CHUNK.type,
                messageId = messageId,
                chunkData = chunkData,
                chunkIndex = chunkIndex
            )
        }
    }
}
