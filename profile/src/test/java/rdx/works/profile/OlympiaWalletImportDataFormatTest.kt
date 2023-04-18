package rdx.works.profile

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeBase64
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import java.io.File

internal class OlympiaWalletExportFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var testVectors: List<TestVector>

    private val parser = OlympiaWalletDataParser()

    @Before
    fun setUp() {
        val testVectorsContent = File("src/test/resources/raw/import_olympia_wallet_parse_test.json").readText()
        testVectors = json.decodeFromString(testVectorsContent)
    }

    @Test
    fun `run tests for test vector`() {

        testVectors.forEach { testVector ->
            val parsedOlympiaAccountData = parser.parseOlympiaWalletAccountData(testVector.payloads)
            Assert.assertNotNull(parsedOlympiaAccountData)
            assert(testVector.olympiaWallet.mnemonic.split(" ").size == parsedOlympiaAccountData!!.mnemonicWordCount)
            parsedOlympiaAccountData.accountData.forEach { olympiaAccountDetail ->
                val correspondingTestVector = testVector.olympiaWallet.accounts[olympiaAccountDetail.index]
                assert(olympiaAccountDetail.accountName == correspondingTestVector.name.orEmpty())
                val pubKeyUnwrapped = correspondingTestVector.pubKey.decodeBase64()?.hex()
                assert(olympiaAccountDetail.publicKey == pubKeyUnwrapped)
            }
        }
    }

    @Test
    fun `incomplete payload parsing return null`() {
        val parsedData = parser.parseOlympiaWalletAccountData(testVectors[1].payloads.subList(0, 1))
        Assert.assertNull(parsedData)
    }

}

@kotlinx.serialization.Serializable
data class TestVector(
    @kotlinx.serialization.SerialName("testID")
    val testID: Int,
    @kotlinx.serialization.SerialName("payloads")
    val payloads: List<String>,
    @kotlinx.serialization.SerialName("numberOfPayloads")
    val numberOfPayloads: Int,
    @kotlinx.serialization.SerialName("olympiaWallet")
    val olympiaWallet: OlympiaWallet
)

@kotlinx.serialization.Serializable
data class OlympiaWallet(
    @kotlinx.serialization.SerialName("accounts")
    val accounts: List<OlympiaAccountTextVector>,
    @kotlinx.serialization.SerialName("mnemonic")
    val mnemonic: String
)

@kotlinx.serialization.Serializable
data class OlympiaAccountTextVector(
    @kotlinx.serialization.SerialName("pubKey")
    val pubKey: String,
    @kotlinx.serialization.SerialName("accountType")
    val accountType: String,
    @kotlinx.serialization.SerialName("addressIndex")
    val addressIndex: Int,
    @kotlinx.serialization.SerialName("name")
    val name: String? = null
)