package rdx.works.profile.data.model.pernetwork

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PerNetwork(
    /**
     * Accounts created by the user for this network.
     */
    @SerialName("accounts")
    val accounts: List<Account>,

    /**
     * ConnectedDapp the user has connected with on this network.
     */
    @SerialName("connectedDapps")
    val connectedDapps: List<String>,

    /**
     * The ID of the network that has been used to generate the accounts, to which personas
     * have been added and dApps connected.
     */
    @SerialName("networkID")
    val networkID: Int,

    /**
     * Personas created by the user for this network.
     */
    @SerialName("personas")
    val personas: List<Persona>
)
