package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.pernetwork.Network

@Serializable
@SerialName("device")
data class DeviceFactorSource(
    override val id: FactorSourceID.FromHash,
    override val common: Common,
    @SerialName("hint")
    val hint: Hint,
    @SerialName("nextDerivationIndicesPerNetwork")
    val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>? = null
) : FactorSource() {

    companion object {

        fun babylon(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = model,
            name = name,
            isOlympiaCompatible = false
        )

        fun olympia(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = model,
            name = name,
            isOlympiaCompatible = true
        )

        private fun device(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
            isOlympiaCompatible: Boolean
        ) = DeviceFactorSource(
            id = FactorSourceID.FromHash(
                kind = FactorSourceKind.DEVICE,
                body = HexCoded32Bytes(
                    value = factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
                ),
            ),
            common = Common(
                cryptoParameters = if (isOlympiaCompatible) {
                    Common.CryptoParameters.olympiaBackwardsCompatible
                } else {
                    Common.CryptoParameters.babylon
                },
                addedOn = InstantGenerator(),
                lastUsedOn = InstantGenerator()
            ),
            hint = Hint(
                model = model,
                name = name
            ),
            nextDerivationIndicesPerNetwork = if (isOlympiaCompatible) null else listOf()
        )
    }
}
