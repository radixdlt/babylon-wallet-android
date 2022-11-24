package rdx.works.profile.model.factorsources

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.bip44.BIP44_PREFIX
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.toKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.hashToFactorId
import java.util.Date

@Serializable
data class FactorSources(
    @SerialName("curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources")
    val curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources
    : List<Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource>,

    @SerialName("secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources")
    val secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources
    : List<Secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources>
) {
    @Serializable
    data class Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource(
        @SerialName("creationDate")
        val creationDate: String,

        @SerialName("factorSourceID")
        val factorSourceID: String,

        @SerialName("label")
        val label: String
    ) {
        companion object {
            fun deviceFactorSource(
                mnemonic: MnemonicWords,
                bip39Passphrase: String = "",
                label: String = "DeviceFactorSource",
                creationDate: String = Date().toString()
            ): Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource {
                val seed = mnemonic.toSeed(bip39Passphrase)
                val derivedKey = seed.toKey(BIP44_PREFIX, EllipticCurveType.Ed25519)

                /**
                 * We get compressed public key which has 33 bytes instead of 32 because slip-10 formats it like this
                 * (by adding zero byte at front, to be the same size as for Secp256k1 elliptic curve)
                 * Then we hash it with sha256 twice and get hex version.
                 */
                val factorSourceID = derivedKey.keyPair.getCompressedPublicKey().hashToFactorId()

                return Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource(
                    creationDate = creationDate,
                    factorSourceID = factorSourceID,
                    label = label
                )
            }
        }
    }

    //TODO secp256k1 not supported for now, but it will be later on
    @Serializable
    data class Secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources(
        @SerialName("creationDate")
        val creationDate: String,

        @SerialName("factorSourceID")
        val factorSourceID: String,

        @SerialName("label")
        val label: String
    )
}