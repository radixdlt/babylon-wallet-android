package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.pernetwork.Network
import java.time.Instant

@Serializable
@SerialName("ledgerHQHardwareWallet")
data class LedgerHardwareWalletFactorSource(
    override val id: FactorSourceID.FromHash,
    override val common: Common,
    @SerialName("hint")
    val hint: Hint,
    // TODO MFA remove (should not be able to create accounts using ledger when MFA)
    @SerialName("nextDerivationIndicesPerNetwork")
    val nextDerivationIndicesPerNetwork: List<Network.NextDerivationIndices>? = null
) : FactorSource() {

    @Serializable
    enum class SigningDisplayMode {
        @SerialName("verbose") Verbose,
        @SerialName("summary") Summary
    }

    enum class DeviceModel(val value: String) {
        NANO_S("nanoS"),
        NANO_S_PLUS("nanoS+"),
        NANO_X("nanoX")
    }

    companion object {

        fun newSource(
            model: DeviceModel,
            name: String,
            deviceID: HexCoded32Bytes,
            createdAt: Instant = InstantGenerator()
        ): LedgerHardwareWalletFactorSource {
            return LedgerHardwareWalletFactorSource(
                id = FactorSourceID.FromHash(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    body = deviceID
                ),
                common = Common(
                    cryptoParameters = Common.CryptoParameters.olympiaBackwardsCompatible,
                    addedOn = createdAt,
                    lastUsedOn = createdAt
                ),
                hint = Hint(
                    name = name,
                    model = model.value
                ),
                nextDerivationIndicesPerNetwork = listOf()
            )
        }
    }
}
