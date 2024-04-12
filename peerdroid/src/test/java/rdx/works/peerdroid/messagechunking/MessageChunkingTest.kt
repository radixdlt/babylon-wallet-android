package rdx.works.peerdroid.messagechunking

import com.radixdlt.sargon.extensions.hex
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import rdx.works.core.hash
import rdx.works.peerdroid.data.PackageDto
import kotlin.random.Random

@Ignore("refactored the MessageChunkingManager")
class MessageChunkingTest {

    @Test
    fun givenOneMbMessageArray_WhenItsSplitAndAssembled_VerifyAssembleSuccessWithCorrectMessageId() {
        // given
        val oneMb = 1024 * 1024
        val oneMbByteArray = Random.nextBytes(oneMb)
        val hashOfMessage = "abc".toByteArray().hash().hex

        // when
        val chunks = splitMessage(oneMbByteArray)
        val messageId = chunks.filterIsInstance<PackageDto.MetaData>().first().messageId
        val assembledMessage = assembleChunks(
            messageId,
            chunks.filterIsInstance<PackageDto.Chunk>()
        )
        val result = verifyAssembledMessage(assembledMessage, hashOfMessage)

        // then
        assert(result.isSuccess)
        Assert.assertEquals(messageId, result.getOrNull())
    }

    @Test
    fun givenOneMbMessageArray_WhenItsSplitAndAssembled_VerifyAssembledMessageIsEqualInitialMessage() {
        // given
        val oneMb = 1024 * 1024
        val oneMbByteArray = Random.nextBytes(oneMb)

        // when
        val chunks = splitMessage(oneMbByteArray)

        val assembledMessage = assembleChunks("messageId", chunks.filterIsInstance<PackageDto.Chunk>())

        // then
        val areEqual = oneMbByteArray.contentEquals(assembledMessage)
        Assert.assertTrue(areEqual)
    }

    /*@Test
    fun givenOneMbMessageArray_WhenItsSplitAndHashIsTampered_VerifyErrorOnAssemble() {
        // given
        val oneMb = 1024 * 1024

        val oneMbByteArray = Random.nextBytes(oneMb)

        // when
        val chunks = splitMessage(oneMbByteArray).toMutableList()
        val metaData = chunks.filterIsInstance<PackageDto.MetaData>().first()
        chunks[0] = PackageDto.MetaData(
            messageId = metaData.messageId,
            chunkCount = metaData.chunkCount,
            hashOfMessage = "abc".toByteArray().sha256Hash().toHexString(),
            messageByteCount = metaData.messageByteCount
        )

        val assembledMessage = assembleChunks(metaData.messageId, chunks)
        val result = verifyAssembledMessage(assembledMessage, chunks)

        // then
        assert(result is Result.Error)
        Assert.assertEquals(metaData.messageId, (result as Result.Error).data)
    }*/
}
