package rdx.works.peerdroid.messagechunking

import io.ktor.util.encodeBase64
import kotlinx.coroutines.runBlocking
import org.junit.Test
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PackageDto
import kotlin.test.assertEquals

class MessageAssemblerTest {

    @Test
    fun `verify a text message is properly assembled to package message`() = runBlocking {
        val textMessage = "this is a test message"
        val actualHashOfMessageInHexString = "afa5476b53c5a84d311c3ed8ff8f77e4990fe190e52cfeba745df6f67c83b7c6"
        val actualChunkData = "dGhpcyBpcyBhIHRlc3QgbWVzc2FnZQ=="

        val expectedHashOfMessage = textMessage.blake2Hash().toHexString()
        assertEquals(expectedHashOfMessage, actualHashOfMessageInHexString)

        val expectedChunkData = textMessage.encodeBase64()
        assertEquals(expectedChunkData, actualChunkData)

        val chunkPackage = PackageDto.Chunk(
            messageId = "messageId",
            chunkIndex = 0,
            chunkData = expectedChunkData
        )
        val listOfChunks = listOf(chunkPackage)

        val assembledMessage = assembleChunks(
            messageId = "messageId",
            chunks = listOfChunks
        )

        val result = verifyAssembledMessage(
            assembledMessage = assembledMessage,
            expectedHashOfMessage = actualHashOfMessageInHexString
        )

        assert(result.isSuccess)
    }
}
