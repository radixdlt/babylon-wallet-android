package rdx.works.peerdroid.messagechunking

import io.ktor.util.encodeBase64
import rdx.works.core.UUIDGenerator
import rdx.works.peerdroid.data.PackageDto
import timber.log.Timber

fun ByteArray.splitMessage(chunkSize: Int): List<PackageDto> {
    val messageSize = size
    val packages = mutableListOf<PackageDto>()
    val messageId = UUIDGenerator.uuid().toString()

    Timber.d("📯 🧱 ready to split message for byte array size: $messageSize")

    if (messageSize > chunkSize) {
        val chunkedSequence = chunkedSequence(chunkSize)
        chunkedSequence.forEachIndexed { index, bytes ->
            packages.add(
                PackageDto.Chunk(
                    messageId = messageId,
                    chunkIndex = index,
                    chunkData = bytes.encodeBase64()
                )
            )
        }
        packages.add(
            index = 0,
            PackageDto.MetaData(
                messageId = messageId,
                chunkCount = packages.count(),
                hashOfMessage = sha256().toHexString(),
                messageByteCount = messageSize
            )
        )
    } else {
        packages.add(
            PackageDto.MetaData(
                messageId = messageId,
                chunkCount = 1,
                hashOfMessage = sha256().toHexString(),
                messageByteCount = messageSize
            )
        )
        packages.add(
            PackageDto.Chunk(
                messageId = messageId,
                chunkIndex = 0,
                chunkData = encodeBase64()
            )
        )
    }

    Timber.d("📯 🧱 split of message completed!")
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
