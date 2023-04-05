package rdx.works.peerdroid.messagechunking

import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rdx.works.core.UUIDGenerator
import rdx.works.core.sha256Hash
import rdx.works.peerdroid.domain.BasePackage

suspend fun ByteArray.splitMessage(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    chunkSize: Int
): List<BasePackage> {
    val messageSize = size
    val packages = mutableListOf<BasePackage>()
    val messageId = UUIDGenerator.uuid().toString()

    withContext(dispatcher) {
        if (messageSize > chunkSize) {
            val chunkedSequence = chunkedSequence(chunkSize)
            chunkedSequence.forEachIndexed { index, bytes ->
                packages.add(
                    BasePackage.ChunkPackage(
                        messageId = messageId,
                        chunkIndex = index,
                        chunkData = bytes.encodeBase64()
                    )
                )
            }
            packages.add(
                0,
                BasePackage.MetadataPackage(
                    messageId = messageId,
                    chunkCount = packages.count(),
                    hashOfMessage = sha256Hash(),
                    messageByteCount = messageSize
                )
            )
        } else {
            packages.add(
                BasePackage.MetadataPackage(
                    messageId = messageId,
                    chunkCount = 1,
                    hashOfMessage = sha256Hash(),
                    messageByteCount = messageSize
                )
            )
            packages.add(
                BasePackage.ChunkPackage(
                    messageId = messageId,
                    chunkIndex = 0,
                    chunkData = encodeBase64()
                )
            )
        }
    }
    return packages
}

private fun ByteArray.chunkedSequence(chunk: Int): Sequence<ByteArray> {
    val input = inputStream()
    val buffer = ByteArray(chunk)
    return generateSequence {
        val bytes = input.read(buffer)
        if (bytes >= 0) {
            buffer.copyOf(bytes)
        } else {
            input.close()
            null
        }
    }
}
