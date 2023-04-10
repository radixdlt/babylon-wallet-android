package rdx.works.peerdroid.messagechunking

import io.ktor.util.decodeBase64Bytes
import rdx.works.peerdroid.data.PackageDto
import timber.log.Timber

fun List<PackageDto.Chunk>.assembleChunks(messageId: String): ByteArray {
    Timber.d("📯 🧱 ready to assemble chunks for messageId = $messageId with list size: $size")
    var assembledMessage = byteArrayOf()

    val filterChunksBasedOnMessageId = filter { it.messageId == messageId }
    filterChunksBasedOnMessageId
        .asSequence()
        .sortedBy { it.chunkIndex }
        .forEach { chunkPackage ->
            val chunkDataByteArray = chunkPackage.chunkData.decodeBase64Bytes()
            assembledMessage = assembledMessage.plus(chunkDataByteArray)
        }

    Timber.d("📯 🧱 assembling of packages completed!")
    return assembledMessage
}
