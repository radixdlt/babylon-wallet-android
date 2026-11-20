package rdx.works.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An iroh relay server added by the user.
 */
@Serializable
data class IrohRelay(
    @SerialName("url")
    val url: String
)

/**
 * The iroh relay servers known to the wallet, like `SavedP2PTransportProfiles` for the signaling servers.
 *
 * The default, n0's public relays (each end uses its closest one), is never stored: [current] is null while they are
 * used, and they are implicitly one of the others otherwise.
 */
@Serializable
data class SavedIrohRelays(
    @SerialName("current")
    val current: IrohRelay? = null,
    @SerialName("others")
    val others: List<IrohRelay> = emptyList()
)
