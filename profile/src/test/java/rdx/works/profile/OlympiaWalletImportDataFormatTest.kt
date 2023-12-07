@file:OptIn(ExperimentalCoroutinesApi::class)

package rdx.works.profile

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeBase64
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import rdx.works.profile.data.model.DeviceInfo
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import java.io.File
import java.time.Instant

internal class OlympiaWalletExportFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var testVectors: List<TestVector>

    private val getCurrentGatewayUseCase = mockk<GetCurrentGatewayUseCase>()
    private val getProfileUseCase = mockk<GetProfileUseCase>()

    private val parser = OlympiaWalletDataParser(getCurrentGatewayUseCase)

    @Before
    fun setUp() {
        coEvery { getCurrentGatewayUseCase() } returns Radix.Gateway("", Radix.Gateway.default.network)
        coEvery { getProfileUseCase() } returns flowOf(
            Profile.init("", DeviceInfo(name = "", manufacturer = "", model = ""), Instant.now())
        )
        val testVectorsContent = File("src/test/resources/raw/import_olympia_wallet_parse_test.json").readText()
        testVectors = json.decodeFromString(testVectorsContent)
    }

    @Test
    fun `run tests for test vector`() = runTest {
        testVectors.forEach { testVector ->
            val parsedOlympiaAccountData = parser.parseOlympiaWalletAccountData(testVector.payloads)
            Assert.assertNotNull(parsedOlympiaAccountData)
            assert(testVector.olympiaWallet.mnemonic.split(MnemonicWithPassphrase.mnemonicWordsDelimiter).size == parsedOlympiaAccountData!!.mnemonicWordCount)
            parsedOlympiaAccountData.accountData.forEach { olympiaAccountDetail ->
                val correspondingTestVector = testVector.olympiaWallet.accounts[olympiaAccountDetail.index]
                assert(
                    olympiaAccountDetail.accountName == correspondingTestVector.name.orEmpty()
                        .ifEmpty { "Unnamed Olympia account ${olympiaAccountDetail.index}" })
                val pubKeyUnwrapped = correspondingTestVector.pubKey.decodeBase64()?.hex()
                assert(olympiaAccountDetail.publicKey == pubKeyUnwrapped)
            }
        }
    }

    @Test
    fun `incomplete payload parsing return null`() = runTest {
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
