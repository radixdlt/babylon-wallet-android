package rdx.works.profile

import com.radixdlt.sargon.Cap26KeyKind
import com.radixdlt.sargon.DerivationPath
import com.radixdlt.sargon.HierarchicalDeterministicPublicKey
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Slip10Curve
import com.radixdlt.sargon.extensions.account
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.identity
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toBagOfBytes
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rdx.works.core.sargon.derivePrivateKey
import rdx.works.core.sargon.derivePublicKey
import rdx.works.core.sargon.init
import java.io.File
import kotlin.test.Test

class DerivationPathTest {

    @Test
    fun `test derivation paths`() {
        val testVectors: List<TestVector> = listOf(
            Json.decodeFromString(File(FILE_CURVE_25519).readText()),
            Json.decodeFromString(File(FILE_SECP_256K1).readText())
        )

        testVectors.forEach { testVector ->
            val mnemonic = MnemonicWithPassphrase.init(
                phrase = testVector.mnemonic
            )

            testVector.tests.forEachIndexed { index, testItem ->
                val resultPath = testItem.toDerivationPath(testVector.network.networkIDDecimal)

                val publicKey = mnemonic.derivePublicKey(
                    derivationPath = resultPath,
                    curve = testVector.slip10Curve
                )
                val privateKey = mnemonic.derivePrivateKey(
                    hdPublicKey = HierarchicalDeterministicPublicKey(
                        publicKey = publicKey,
                        derivationPath = resultPath
                    )
                )

                assertEquals("${testVector.slip10Curve}: Test[${index}] Path is the same", testItem.path, resultPath.string)
                assertEquals("${testVector.slip10Curve}: Test[${index}] PublicKey is the same", testItem.publicKey, publicKey.hex)
                assertEquals(
                    "${testVector.slip10Curve}: Test[${index}] PrivateKey is the same",
                    testItem.privateKey,
                    privateKey.toByteArray().toBagOfBytes().hex
                )
            }
        }
    }

    @Serializable
    private data class TestVector(
        val mnemonic: String,
        val tests: List<TestItem>,
        private val curve: String,
        val network: TestVectorNetwork
    ) {

        val slip10Curve: Slip10Curve
            get() = when (curve) {
                "curve25519" -> Slip10Curve.CURVE25519
                "secp256k1" -> Slip10Curve.SECP256K1
                else -> throw RuntimeException("Unknown curve $curve")
            }

    }

    @Serializable
    private data class TestVectorNetwork(
        val name: String,
        val networkIDDecimal: Int
    )

    @Serializable
    private data class TestItem(
        val path: String,
        val publicKey: String,
        val entityKind: Int,
        val privateKey: String,
        val entityIndex: Int,
        val keyKind: Int
    ) {

        fun toDerivationPath(
            networkIdValue: Int
        ): DerivationPath {
            val networkId = NetworkId.init(discriminant = networkIdValue.toUByte())
            val cap26KeyKind =
                Cap26KeyKind.entries.find { it.value == keyKind.toUInt() } ?: error("$keyKind is not a valid Cap26KeyKind")
            return when (entityKind.toUInt()) {
                525u -> DerivationPath.Cap26.account(
                    networkId = networkId,
                    keyKind = cap26KeyKind,
                    index = entityIndex.toUInt()
                )

                618u -> DerivationPath.Cap26.identity(
                    networkId = networkId,
                    keyKind = cap26KeyKind,
                    index = entityIndex.toUInt()
                )

                else -> error("$entityKind is not and Account not an Identity EntityType")
            }
        }

    }

    companion object {
        private const val FILE_CURVE_25519 = "src/test/resources/raw/cap26_curve25519.json"
        private const val FILE_SECP_256K1 = "src/test/resources/raw/cap26_secp256k1.json"
    }
}
