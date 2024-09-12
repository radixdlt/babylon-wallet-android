@file:OptIn(ExperimentalCoroutinesApi::class)

package rdx.works.profile

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeBase64
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.olympiaimport.OlympiaWalletDataParser
import java.io.File
import kotlin.test.Test

internal class OlympiaWalletExportFormatTest {

    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var testVectors: List<TestVector>

    private val profileRepository = FakeProfileRepository(Profile.sample())
    private val getProfileUseCase = GetProfileUseCase(profileRepository)

    private val parser = OlympiaWalletDataParser(getProfileUseCase)

    @Before
    fun setUp() {
        val testVectorsContent = File("src/test/resources/raw/import_olympia_wallet_parse_test.json").readText()
        testVectors = json.decodeFromString(testVectorsContent)
    }

    @Test
    fun `run tests for test vector`() = runTest {
        testVectors.forEach { testVector ->
            val parsedOlympiaAccountData = parser.parseOlympiaWalletAccountData(testVector.payloads)
            assertNotNull(parsedOlympiaAccountData)
            assert(testVector.olympiaWallet.mnemonic.split(" ").size == parsedOlympiaAccountData!!.mnemonicWordCount.value.toInt())
            parsedOlympiaAccountData.accountData.forEach { olympiaAccountDetail ->
                val correspondingTestVector = testVector.olympiaWallet.accounts[olympiaAccountDetail.index]
                assert(
                    olympiaAccountDetail.accountName == correspondingTestVector.name.orEmpty()
                        .ifEmpty { "Unnamed Olympia account ${olympiaAccountDetail.index}" })

                assert(olympiaAccountDetail.publicKey == correspondingTestVector.publicKey)
            }
        }
    }

    @Test
    fun `incomplete payload parsing return null`() = runTest {
        val parsedData = parser.parseOlympiaWalletAccountData(testVectors[1].payloads.subList(0, 1))
        assertNull(parsedData)
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
) {

    val publicKey: PublicKey
        get() = PublicKey.init(pubKey.decodeBase64()?.hex().orEmpty())

}
