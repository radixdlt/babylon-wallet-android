package rdx.works.peerdroid.messagechunking

import io.ktor.util.decodeBase64String
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import rdx.works.peerdroid.domain.BasePackage
import rdx.works.peerdroid.helpers.sha256
import rdx.works.peerdroid.helpers.toHexString
import kotlin.random.Random
import kotlin.test.assertEquals

class MessageSplitterTest {

    @Test
    fun `verify a byte array which contains a test message is properly split to a list of packages`() = runBlocking {
        val textMessage = "this is a test message"
        val textMessageByteArray = textMessage.toByteArray()

        val actualSizeOfPackages = 2
        val actualHashOfMessageInHexString = "4e4aa09b6d80efbd684e80f54a70c1d8605625c3380f4cb012b32644a002b5be"
        val actualMessageByteCount = 22
        val actualChunkData = "dGhpcyBpcyBhIHRlc3QgbWVzc2FnZQ=="

        val listOfPackages = splitMessage(textMessageByteArray)
        assertEquals(listOfPackages.size, actualSizeOfPackages)

        assert(listOfPackages[0] is BasePackage.MetadataPackage)
        val metadataPackage = listOfPackages[0] as BasePackage.MetadataPackage

        val expectedHashOfMessageInHexString = metadataPackage.hashOfMessage.toHexString()
        assertEquals(expectedHashOfMessageInHexString, actualHashOfMessageInHexString)

        val expectedMessageByteCount = metadataPackage.messageByteCount
        assertEquals(expectedMessageByteCount, actualMessageByteCount)

        assert(listOfPackages[1] is BasePackage.ChunkPackage)
        val chunkPackage = listOfPackages[1] as BasePackage.ChunkPackage

        val expectedChunkData = chunkPackage.chunkData
        assertEquals(expectedChunkData, actualChunkData)
        assertEquals(expectedChunkData.decodeBase64String(), textMessage)
    }

    @Test
    fun givenByteArrayIsLargerThanChunkSize_WhenSplit_VerifyChunkCountIsCorrect() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 20
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertEquals(metadataPackage.chunkCount, byteArraySize / chunkSize)
    }

    @Test
    fun givenByteArrayIsLargerThanChunkSize_WhenSplit_VerifyHashOfMessageIsCorrect() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 20
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertTrue(metadataPackage.hashOfMessage.contentEquals(byteArray.sha256()))
    }

    @Test
    fun givenByteArrayIsLargerThanChunkSize_WhenSplit_VerifyMessageByteCountIsCorrect() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 20
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertEquals(metadataPackage.messageByteCount, byteArraySize)
    }

    @Test
    fun givenByteArrayIsSmallerThanChunkSize_WhenSplit_VerifyChunkCountIsOne() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 120
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertEquals(metadataPackage.chunkCount, 1)
    }

    @Test
    fun givenByteArrayIsSmallerThanChunkSize_WhenSplit_VerifyHashOfMessageIsCorrect() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 120
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertTrue(metadataPackage.hashOfMessage.contentEquals(byteArray.sha256()))
    }

    @Test
    fun givenByteArrayIsSmallerThanChunkSize_WhenSplit_VerifyMessageByteCountIsCorrect() = runBlocking {
        // given
        val byteArraySize = 100
        val chunkSize = 120
        val byteArray = Random.nextBytes(byteArraySize)

        // when
        val result = byteArray.splitMessage(chunkSize = chunkSize)

        // then
        val metadataPackage = result.first() as BasePackage.MetadataPackage

        Assert.assertEquals(metadataPackage.messageByteCount, byteArraySize)
    }
}
