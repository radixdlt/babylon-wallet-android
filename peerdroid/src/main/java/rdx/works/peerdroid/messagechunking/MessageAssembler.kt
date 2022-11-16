package rdx.works.peerdroid.messagechunking

import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.domain.BasePackage

suspend fun List<BasePackage>.assembleChunks(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): ByteArray {
    var assembledMessage = byteArrayOf()

    val metadataChunkList = filterIsInstance<BasePackage.MetadataPackage>()
    if (metadataChunkList.size > 1) {
        error("Two metadata chunks, data corrupted")
    }

    val metadataChunk = metadataChunkList.first()
    if (metadataChunk.chunkCount != size - 1) {
        error("Not enough chunks, data corrupted")
    }

    withContext(dispatcher) {
        val messageId = metadataChunk.messageId
        val idFilteredChunks = filter { it.messageId == messageId }
        idFilteredChunks
            .asSequence()
            .filterIsInstance<BasePackage.ChunkPackage>()
            .sortedBy { it.chunkIndex }
            .forEach { chunkPackage ->
                val chunkDataByteArray = chunkPackage.chunkData.decodeBase64Bytes()
                assembledMessage = assembledMessage.plus(chunkDataByteArray)
            }
    }

    return assembledMessage
}
