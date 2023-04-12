package rdx.works.peerdroid.messagechunking

import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber

private const val ERROR_MESSAGE_HASHES_MISMATCH = "Message hashes mismatch"
private const val CHUNK_SIZE = 15441

fun splitMessage(messageInByteArray: ByteArray): List<PackageDto> = messageInByteArray.splitMessage(
    chunkSize = CHUNK_SIZE
)

fun assembleChunks(
    messageId: String,
    chunks: List<PackageDto.Chunk>
): ByteArray = chunks.assembleChunks(messageId)

fun verifyAssembledMessage(
    assembledMessage: ByteArray,
    expectedHashOfMessage: String
): Result<Unit> {
    return try {
        if (!expectedHashOfMessage.contentEquals(assembledMessage.blake2Hash().toHexString())) {
            Timber.d("📯 🧱 failed to verify hash of assembled message: hash mismatch")
            Result.Error(
                message = ERROR_MESSAGE_HASHES_MISMATCH,
                data = ""
            )
        }

        Timber.d("📯 🧱 hash of assembled message verified successfully")
        Result.Success(Unit)
    } catch (exception: Exception) {
        Timber.e("📯 🧱 failed to verify hash of assembled message: ${exception.localizedMessage}")
        Result.Error(message = "no metadata chunk")
    }
}
