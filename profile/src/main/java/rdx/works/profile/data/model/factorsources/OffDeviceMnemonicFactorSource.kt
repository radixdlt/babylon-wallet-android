package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import java.time.Instant

@Serializable
@SerialName("offDeviceMnemonic")
data class OffDeviceMnemonicFactorSource(
    override val id: FactorSourceID.FromHash,
    override val common: Common,
    @SerialName("bip39Parameters")
    val bip39Parameters: Bip39Parameters,
    @SerialName("hint")
    val hint: Hint,
) : FactorSource() {

    @Serializable
    data class Bip39Parameters(
        @SerialName("bip39PassphraseSpecified")
        val bip39PassphraseSpecified: Boolean,
        @SerialName("language")
        val language: String,
        @SerialName("wordCount")
        val wordCount: Int
    )

    @Serializable
    data class Hint(
        @SerialName("label")
        val label: String
    )

    companion object {

        fun newSource(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            label: String = "",
            createdAt: Instant = InstantGenerator()
        ): OffDeviceMnemonicFactorSource {
            return OffDeviceMnemonicFactorSource(
                id = FactorSourceID.FromHash(
                    kind = FactorSourceKind.OFF_DEVICE_MNEMONIC,
                    body = HexCoded32Bytes(
                        value = factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
                    )
                ),
                common = Common(
                    cryptoParameters = Common.CryptoParameters.trustedEntity,
                    addedOn = createdAt,
                    lastUsedOn = createdAt
                ),
                bip39Parameters = Bip39Parameters(
                    bip39PassphraseSpecified = mnemonicWithPassphrase.bip39Passphrase.isNotEmpty(),
                    language = "", // TODO we must add enumerator in SLIP10
                    wordCount = 1, // TODO we must add enumerator in SLIP10
                ),
                hint = Hint(label = label),
            )
        }
    }
}
