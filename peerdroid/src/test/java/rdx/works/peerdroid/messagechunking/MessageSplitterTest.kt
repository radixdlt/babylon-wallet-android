package rdx.works.peerdroid.messagechunking

import com.radixdlt.sargon.extensions.hex
import io.ktor.util.decodeBase64String
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import rdx.works.core.hash
import rdx.works.peerdroid.data.PackageDto
import kotlin.random.Random
import kotlin.test.assertEquals

class MessageSplitterTest {

    @Test
    fun `verify a byte array which contains a test message is properly split to a list of packages`() = runBlocking {
        val textMessage = "this is a test message"
        val textMessageByteArray = textMessage.toByteArray()

        val actualSizeOfPackages = 2
        val actualHashOfMessageInHexString = "afa5476b53c5a84d311c3ed8ff8f77e4990fe190e52cfeba745df6f67c83b7c6"
        val actualMessageByteCount = 22
        val actualChunkData = "dGhpcyBpcyBhIHRlc3QgbWVzc2FnZQ=="

        val listOfPackages = splitMessage(textMessageByteArray)
        assertEquals(listOfPackages.size, actualSizeOfPackages)

        assert(listOfPackages[0] is PackageDto.MetaData)
        val metadataPackage = listOfPackages[0] as PackageDto.MetaData

        val expectedHashOfMessageInHexString = metadataPackage.hashOfMessage//.toHexString()
        assertEquals(expectedHashOfMessageInHexString, actualHashOfMessageInHexString)

        val expectedMessageByteCount = metadataPackage.messageByteCount
        assertEquals(expectedMessageByteCount, actualMessageByteCount)

        assert(listOfPackages[1] is PackageDto.Chunk)
        val chunkPackage = listOfPackages[1] as PackageDto.Chunk

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
        val metadataPackage = result.first() as PackageDto.MetaData

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
        val metadataPackage = result.first() as PackageDto.MetaData

        Assert.assertTrue(metadataPackage.hashOfMessage.contentEquals(byteArray.hash().hex))
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
        val metadataPackage = result.first() as PackageDto.MetaData

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
        val metadataPackage = result.first() as PackageDto.MetaData

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
        val metadataPackage = result.first() as PackageDto.MetaData

        Assert.assertTrue(metadataPackage.hashOfMessage.contentEquals(byteArray.hash().hex))
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
        val metadataPackage = result.first() as PackageDto.MetaData

        Assert.assertEquals(metadataPackage.messageByteCount, byteArraySize)
    }
}
