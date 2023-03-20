package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Serializable
internal data class ProfileSnapshot(
    /**
     * Locally generated identifier, based on the [Profile]'s id which is based upon.
     */
    @SerialName("id")
    private val id: String,

    /**
     * The name of the device from which this snapshot was saved from.
     */
    @SerialName("creatingDevice")
    private val creatingDevice: String,

    /**
     * Settings for this profile in the app, contains default security configs as well as display settings.
     */
    @SerialName("appPreferences")
    private val appPreferences: AppPreferences,

    /**
     * The known sources of factors, used for authorization such as spending funds.
     * Always contains at least one DeviceFactorSource.
     */
    @SerialName("factorSources")
    private val factorSources: List<FactorSource>,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    @SerialName("perNetwork")
    private val onNetwork: List<OnNetwork>,

    /**
     * Incrementing from 1
     */
    @SerialName("version")
    private val version: Int
) {

    fun toProfile(): Profile {
        return Profile(
            id = id,
            creatingDevice = creatingDevice,
            appPreferences = appPreferences,
            factorSources = factorSources,
            onNetwork = onNetwork,
            version = version
        )
    }

    @Serializable
    internal data class ProfileVersionHolder(
        @SerialName("version")
        val version: Int
    )
}
