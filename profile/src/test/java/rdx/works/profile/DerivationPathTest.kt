package rdx.works.profile

import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.extensions.removeLeadingZero
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import rdx.works.core.toHexString
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.factorsources.Slip10Curve
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.toExtendedKey
import rdx.works.profile.derivation.model.EntityType
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import java.io.File
import kotlin.test.assertEquals

class DerivationPathTest {

    @Test
    fun `test derivation paths`() {
        val testVectors: List<TestVector> = listOf(
            Json.decodeFromString(File(FILE_CURVE_25519).readText()),
            Json.decodeFromString(File(FILE_SECP_256K1).readText())
        )

        testVectors.forEach { testVector ->
            val mnemonic = MnemonicWithPassphrase(
                mnemonic = testVector.mnemonic,
                bip39Passphrase = ""
            )

            testVector.tests.forEachIndexed { index, testItem ->
                val actualPath = testItem.toDerivationPath(testVector.network.networkIDDecimal)

                val extendedKey = mnemonic.toExtendedKey(
                    curve = testVector.slip10Curve,
                    derivationPath = actualPath
                )
                val actualPublicKey = extendedKey.keyPair.getCompressedPublicKey().removeLeadingZero().toHexString()
                val actualPrivateKey = extendedKey.keyPair.privateKey.toHexString()

                assertEquals(testItem.path, actualPath.path, "${testVector.slip10Curve}: Test[${index}] Path is the same")
                assertEquals(testItem.publicKey, actualPublicKey, "${testVector.slip10Curve}: Test[${index}] PublicKey is the same")
                assertEquals(testItem.privateKey, actualPrivateKey, "${testVector.slip10Curve}: Test[${index}] PrivateKey is the same")
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
                "curve25519" -> Slip10Curve.CURVE_25519
                "secp256k1" -> Slip10Curve.SECP_256K1
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
            val keyType = KeyType.values().find { it.value == keyKind } ?: throw RuntimeException("KeyType with $keyKind not supported")
            val networkId = NetworkId.values().find {
                it.value == networkIdValue
            } ?: throw RuntimeException("Unknown network id $networkIdValue")
            return when (entityKind) {
                EntityType.Account.value -> DerivationPath.forAccount(
                    networkId = networkId,
                    accountIndex = entityIndex,
                    keyType = keyType
                )
                EntityType.Identity.value -> DerivationPath.forIdentity(
                    networkId = networkId,
                    identityIndex = entityIndex,
                    keyType = keyType
                )
                else -> throw RuntimeException("$entityKind is not and Account not an Identity EntityType")
            }
        }

    }

    companion object {
        private const val FILE_CURVE_25519 = "src/test/resources/raw/cap26_curve25519.json"
        private const val FILE_SECP_256K1 = "src/test/resources/raw/cap26_secp256k1.json"
    }
}
