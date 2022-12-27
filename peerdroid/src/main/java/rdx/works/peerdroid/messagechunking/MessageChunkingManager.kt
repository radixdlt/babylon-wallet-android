package rdx.works.peerdroid.messagechunking

import rdx.works.peerdroid.domain.BasePackage
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.helpers.sha256

private const val ERROR_MESSAGE_HASHES_MISMATCH = "Message hashes mismatch"
private const val CHUNK_SIZE = 15441

suspend fun splitMessage(messageInByteArray: ByteArray): List<BasePackage> = messageInByteArray.splitMessage(
    chunkSize = CHUNK_SIZE
)

suspend fun assembleChunks(chunks: List<BasePackage>): ByteArray = chunks.assembleChunks()

@Suppress("ReturnCount")
fun verifyAssembledMessage(assembledMessage: ByteArray, chunks: List<BasePackage>): Result<String> {
    try {
        val metadataChunk = chunks.filterIsInstance<BasePackage.MetadataPackage>().first()

        if (!metadataChunk.hashOfMessage.contentEquals(assembledMessage.sha256())) {
            return Result.Error(
                message = ERROR_MESSAGE_HASHES_MISMATCH,
                data = metadataChunk.messageId
            )
        }

        return Result.Success(metadataChunk.messageId)
    } catch (noSuchElementException: NoSuchElementException) {
        return Result.Error(
            message = "no metadata chunk",
            data = ""
        )
    }
}
