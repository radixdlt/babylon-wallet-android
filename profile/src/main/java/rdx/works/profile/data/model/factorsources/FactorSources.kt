package rdx.works.profile.data.model.factorsources

import com.radixdlt.bip39.model.MnemonicWords
import com.radixdlt.crypto.ec.EllipticCurveType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.extensions.compressedPublicKey
import rdx.works.profile.data.utils.hashToFactorId
import rdx.works.profile.derivation.CustomHDDerivationPath
import java.time.Instant

@Serializable
data class FactorSources(
    @SerialName("curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources")
    val curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSources:
        List<Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource>,

    @SerialName("secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources")
    val secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources:
        List<Secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources>
) {
    @Serializable
    data class Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource(
        @SerialName("creationDate")
        val creationDate: String,

        @SerialName("factorSourceID")
        val factorSourceID: String,

        @SerialName("label")
        val label: String?
    ) {
        companion object {

            fun deviceFactorSource(
                mnemonic: MnemonicWords,
                bip39Passphrase: String = "",
                label: String?,
                creationDate: String = Instant.now().toString()
            ): Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource {
                val factorSourceID = factorSourceId(
                    mnemonic = mnemonic,
                    ellipticCurveType = EllipticCurveType.Ed25519,
                    derivationPath = CustomHDDerivationPath.getId.path,
                    bip39Passphrase = bip39Passphrase
                )

                return Curve25519OnDeviceStoredMnemonicHierarchicalDeterministicSLIP10FactorSource(
                    creationDate = creationDate,
                    factorSourceID = factorSourceID,
                    label = label
                )
            }
        }
    }

    // TODO secp256k1 not supported for now, but it will be later on
    @Serializable
    data class Secp256k1OnDeviceStoredMnemonicHierarchicalDeterministicBIP44FactorSources(
        @SerialName("creationDate")
        val creationDate: String,

        @SerialName("factorSourceID")
        val factorSourceID: String,

        @SerialName("label")
        val label: String?
    )

    companion object {
        /**
         * This generates the key, in our case factorSourceID, which we can use to associate with mnemonics
         * when we store it for now defaults to Ed25519 curve and getId custom derivation path
         */
        fun factorSourceId(
            mnemonic: MnemonicWords,
            ellipticCurveType: EllipticCurveType = EllipticCurveType.Ed25519,
            derivationPath: String = CustomHDDerivationPath.getId.path,
            bip39Passphrase: String = ""
        ): String {
            return mnemonic.compressedPublicKey(
                ellipticCurveType = ellipticCurveType,
                derivationPath = derivationPath,
                bip39Passphrase = bip39Passphrase
            ).hashToFactorId()
        }
    }
}
