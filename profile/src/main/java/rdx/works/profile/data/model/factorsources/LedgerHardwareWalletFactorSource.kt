package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network

@Serializable
data class LedgerHardwareWalletFactorSource(
    override val id: FactorSource.FactorSourceID.FromHash,
    override val common: FactorSource.Common,
    @SerialName("hint")
    val hint: FactorSource.Hint,
    @SerialName("nextDerivationIndicesPerNetwork")
    val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>? = null
) : FactorSource {

    enum class DeviceModel(val value: String) {
        NANO_S("nanoS"),
        NANO_S_PLUS("nanoS+"),
        NANO_X("nanoX")
    }

    companion object {

        fun newSource(
            model: DeviceModel,
            name: String,
            deviceID: FactorSource.HexCoded32Bytes
        ): LedgerHardwareWalletFactorSource {
            return LedgerHardwareWalletFactorSource(
                id = FactorSource.FactorSourceID.FromHash(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    body = deviceID
                ),
                common = FactorSource.Common(
                    cryptoParameters = FactorSource.Common.CryptoParameters.olympiaBackwardsCompatible,
                    addedOn = InstantGenerator(),
                    lastUsedOn = InstantGenerator()
                ),
                hint = FactorSource.Hint(
                    name = name,
                    model = model.value
                ),
                nextDerivationIndicesPerNetwork = listOf()
            )
        }
    }
}
