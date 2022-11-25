package rdx.works.profile.data.model.factorsources

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.bip39.toSeed
import com.radixdlt.crypto.ec.EllipticCurveType
import com.radixdlt.crypto.getCompressedPublicKey
import com.radixdlt.slip10.toKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.hashToFactorId
import rdx.works.profile.derivation.CustomHDDerivationPath
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
                val getIdPath = CustomHDDerivationPath.getId.path
                val derivedKey = seed.toKey(getIdPath, EllipticCurveType.Ed25519)

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