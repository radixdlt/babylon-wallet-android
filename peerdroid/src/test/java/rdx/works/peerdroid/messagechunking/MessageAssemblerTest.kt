package rdx.works.peerdroid.messagechunking

import io.ktor.util.encodeBase64
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.decodeHex
import org.junit.Test
import rdx.works.core.sha256Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.domain.BasePackage
import rdx.works.peerdroid.helpers.Result
import kotlin.test.assertEquals

class MessageAssemblerTest {

    @Test
    fun `verify a text message is properly assembled to package message`() = runBlocking {
        val textMessage = "this is a test message"
        val actualHashOfMessageInHexString = "4e4aa09b6d80efbd684e80f54a70c1d8605625c3380f4cb012b32644a002b5be"
        val actualMessageByteCount = 22
        val actualMessageChunkCount = 1
        val actualChunkData = "dGhpcyBpcyBhIHRlc3QgbWVzc2FnZQ=="

        val expectedHashOfMessage = textMessage.toByteArray().sha256Hash().toHexString()
        assertEquals(expectedHashOfMessage, actualHashOfMessageInHexString)

        val expectedChunkData = textMessage.encodeBase64()
        assertEquals(expectedChunkData, actualChunkData)

        val metadataPackage = BasePackage.MetadataPackage(
            messageId = "messageId",
            chunkCount = actualMessageChunkCount,
            hashOfMessage = expectedHashOfMessage.decodeHex().toByteArray(),
            messageByteCount = actualMessageByteCount
        )
        val chunkPackage = BasePackage.ChunkPackage(
            messageId = "messageId",
            chunkIndex = 0,
            chunkData = expectedChunkData
        )
        val listOfPackages = listOf(metadataPackage, chunkPackage)

        val assembledMessage = assembleChunks(listOfPackages)

        val result = verifyAssembledMessage(
            assembledMessage = assembledMessage,
            chunks = listOfPackages
        )

        assert(result is Result.Success)
    }

    @Test(expected = IllegalStateException::class)
    fun givenTwoMetadataPackages_whenAssemblingChunks_VerifyExceptionThrown(): Unit = runBlocking {
        val chunks = listOf<BasePackage>(
            BasePackage.MetadataPackage(
                messageId = "",
                chunkCount = 1,
                hashOfMessage = byteArrayOf(1, 4, 5, 7),
                messageByteCount = 123
            ),
            BasePackage.MetadataPackage(
                messageId = "",
                chunkCount = 1,
                hashOfMessage = byteArrayOf(1, 4, 5, 7),
                messageByteCount = 123
            )
        )

        chunks.assembleChunks()
    }

    @Test(expected = IllegalStateException::class)
    fun givenChunkCountIsOneButTwoChunks_whenAssemblingChunks_VerifyExceptionThrown(): Unit = runBlocking {
        val messageId = "1234few"
        val chunks = listOf(
            BasePackage.MetadataPackage(
                messageId = messageId,
                chunkCount = 1,
                hashOfMessage = byteArrayOf(1, 4, 5, 7),
                messageByteCount = 123
            ),
            BasePackage.ChunkPackage(
                messageId = messageId,
                chunkIndex = 1,
                chunkData = "3r2r32"
            ),
            BasePackage.ChunkPackage(
                messageId = messageId,
                chunkIndex = 2,
                chunkData = "3r2rf3232"
            )
        )

        chunks.assembleChunks()
    }

    @Test(expected = IllegalStateException::class)
    fun givenChunkCountIsThreeButTwoChunks_whenAssemblingChunks_VerifyExceptionThrown(): Unit = runBlocking {
        val messageId = "1234few"
        val chunks = listOf(
            BasePackage.MetadataPackage(
                messageId = messageId,
                chunkCount = 3,
                hashOfMessage = byteArrayOf(1, 4, 5, 7),
                messageByteCount = 123
            ),
            BasePackage.ChunkPackage(
                messageId = messageId,
                chunkIndex = 1,
                chunkData = "3r2r32"
            ),
            BasePackage.ChunkPackage(
                messageId = messageId,
                chunkIndex = 2,
                chunkData = "3r2rf3232"
            )
        )

        chunks.assembleChunks()
    }
}
