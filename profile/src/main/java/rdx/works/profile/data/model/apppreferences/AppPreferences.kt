package rdx.works.profile.data.model.apppreferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    @SerialName("display")
    val display: Display,

    @SerialName("security")
    val security: Security,

    @SerialName("gateways")
    val gateways: Gateways,

    @SerialName("p2pLinks")
    val p2pLinks: List<P2PLink>
)
