package rdx.works.peerdroid.messagechunking

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import rdx.works.core.sha256Hash
import rdx.works.peerdroid.domain.BasePackage
import rdx.works.peerdroid.helpers.Result
import kotlin.random.Random

class MessageChunkingTest {

    @Test
    fun givenOneMbMessageArray_WhenItsSplitAndAssembled_VerifyAssembleSuccessWithCorrectMessageId(): Unit = runBlocking {
        // given
        val oneMb = 1024 * 1024
        val oneMbByteArray = Random.nextBytes(oneMb)

        // when
        val chunks = splitMessage(oneMbByteArray)
        val messageId = chunks.filterIsInstance<BasePackage.MetadataPackage>().first().messageId
        val assembledMessage = assembleChunks(chunks)
        val result = verifyAssembledMessage(assembledMessage, chunks)

        // then
        assert(result is Result.Success)
        Assert.assertEquals(messageId, (result as Result.Success).data)
    }

    @Test
    fun givenOneMbMessageArray_WhenItsSplitAndAssembled_VerifyAssembledMessageIsEqualInitialMessage(): Unit = runBlocking {
        // given
        val oneMb = 1024 * 1024
        val oneMbByteArray = Random.nextBytes(oneMb)

        // when
        val chunks = splitMessage(oneMbByteArray)

        val assembledMessage = assembleChunks(chunks)

        // then
        val areEqual = oneMbByteArray.contentEquals(assembledMessage)
        Assert.assertTrue(areEqual)
    }

    @Test
    fun givenOneMbMessageArray_WhenItsSplitAndHashIsTampered_VerifyErrorOnAssemble(): Unit = runBlocking {
        // given
        val oneMb = 1024 * 1024

        val oneMbByteArray = Random.nextBytes(oneMb)

        // when
        val chunks = splitMessage(oneMbByteArray).toMutableList()
        val metaData = chunks.filterIsInstance<BasePackage.MetadataPackage>().first()
        chunks[0] = BasePackage.MetadataPackage(
            messageId = metaData.messageId,
            chunkCount = metaData.chunkCount,
            hashOfMessage = "abc".toByteArray().sha256Hash(),
            messageByteCount = metaData.messageByteCount
        )

        val assembledMessage = assembleChunks(chunks)
        val result = verifyAssembledMessage(assembledMessage, chunks)

        // then
        assert(result is Result.Error)
        Assert.assertEquals(metaData.messageId, (result as Result.Error).data)
    }
}
