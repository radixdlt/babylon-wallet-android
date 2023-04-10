package rdx.works.peerdroid.messagechunking

import io.ktor.util.encodeBase64
import kotlinx.coroutines.runBlocking
import org.junit.Test
import rdx.works.core.sha256Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.helpers.Result
import kotlin.test.assertEquals

class MessageAssemblerTest {

    @Test
    fun `verify a text message is properly assembled to package message`() = runBlocking {
        val textMessage = "this is a test message"
        val actualHashOfMessageInHexString = "4e4aa09b6d80efbd684e80f54a70c1d8605625c3380f4cb012b32644a002b5be"
        val actualChunkData = "dGhpcyBpcyBhIHRlc3QgbWVzc2FnZQ=="

        val expectedHashOfMessage = textMessage.toByteArray().sha256Hash().toHexString()
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

        assert(result is Result.Success)
    }
}
