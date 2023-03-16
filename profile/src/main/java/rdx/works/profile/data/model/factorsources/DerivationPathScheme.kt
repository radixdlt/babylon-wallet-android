package rdx.works.profile.data.model.factorsources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DerivationPathScheme {
    @SerialName("cap26")
    CAP_26,

    @SerialName("bip44Olympia")
    BIP_44_OLYMPIA
}
