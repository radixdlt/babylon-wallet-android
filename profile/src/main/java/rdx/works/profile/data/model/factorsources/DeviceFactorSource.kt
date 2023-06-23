package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import rdx.works.profile.data.model.pernetwork.Network

@Serializable
data class DeviceFactorSource(
    override val id: FactorSource.FactorSourceID.FromHash,
    override val common: FactorSource.Common,
    @SerialName("hint")
    val hint: FactorSource.Hint,
    @SerialName("nextDerivationIndicesPerNetwork")
    val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>? = null
) : FactorSource {

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
            id = FactorSource.FactorSourceID.FromHash(
                kind = FactorSourceKind.DEVICE,
                body = FactorSource.HexCoded32Bytes(
                    value = FactorSource.factorSourceId(mnemonicWithPassphrase = mnemonicWithPassphrase)
                ),
            ),
            common = FactorSource.Common(
                cryptoParameters = if (isOlympiaCompatible) {
                    FactorSource.Common.CryptoParameters.olympiaBackwardsCompatible
                } else {
                    FactorSource.Common.CryptoParameters.babylon
                },
                addedOn = InstantGenerator(),
                lastUsedOn = InstantGenerator()
            ),
            hint = FactorSource.Hint(
                model = model,
                name = name
            ),
            nextDerivationIndicesPerNetwork = if (isOlympiaCompatible) null else listOf()
        )
    }
}
