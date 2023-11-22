package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.core.HexCoded32Bytes
import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.MnemonicWithPassphrase
import java.time.Instant

@Serializable
@SerialName("device")
data class DeviceFactorSource(
    override val id: FactorSourceID.FromHash,
    override val common: Common,
    @SerialName("hint")
    val hint: Hint
) : FactorSource() {

    @Serializable
    data class Hint(
        @SerialName("model")
        val model: String,
        @SerialName("name")
        val name: String,
        @SerialName("mnemonicWordCount")
        val mnemonicWordCount: Int
    )

    val isBabylon: Boolean
        get() = common.cryptoParameters == Common.CryptoParameters.babylon

    val isMainBabylon: Boolean
        get() = common.flags.contains(FactorSourceFlag.Main) && isBabylon

    val isOlympia: Boolean
        get() = common.cryptoParameters == Common.CryptoParameters.olympiaBackwardsCompatible

    companion object {

        fun babylon(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
            createdAt: Instant = InstantGenerator(),
            isMain: Boolean = false
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = model,
            name = name,
            isOlympiaCompatible = false,
            createdAt = createdAt,
            isMain = isMain
        )

        fun olympia(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
            createdAt: Instant = InstantGenerator()
        ) = device(
            mnemonicWithPassphrase = mnemonicWithPassphrase,
            model = model,
            name = name,
            isOlympiaCompatible = true,
            createdAt = createdAt,
            isMain = false
        )

        @Suppress("LongParameterList")
        private fun device(
            mnemonicWithPassphrase: MnemonicWithPassphrase,
            model: String = "",
            name: String = "",
            isOlympiaCompatible: Boolean,
            createdAt: Instant,
            isMain: Boolean = false
        ): DeviceFactorSource {
            require((isMain && isOlympiaCompatible).not()) {
                "Olympia Device factor source should never be marked 'main'."
            }
            return DeviceFactorSource(
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
                    addedOn = createdAt,
                    lastUsedOn = createdAt,
                    flags = if (isMain) listOf(FactorSourceFlag.Main) else emptyList()
                ),
                hint = Hint(
                    model = model,
                    name = name,
                    mnemonicWordCount = mnemonicWithPassphrase.wordCount
                )
            )
        }
    }
}
