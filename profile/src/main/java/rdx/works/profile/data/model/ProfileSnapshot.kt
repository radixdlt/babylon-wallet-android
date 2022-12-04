package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.factorsources.FactorSources
import rdx.works.profile.data.model.pernetwork.PerNetwork

@Serializable
data class ProfileSnapshot(
    /**
     * Settings for this profile in the app, contains default security configs as well as display settings.
     */
    @SerialName("appPreferences")
    val appPreferences: AppPreferences,

    /**
     * The known sources of factors, used for authorization such as spending funds.
     * Always contains at least one DeviceFactorSource.
     */
    @SerialName("factorSources")
    val factorSources: FactorSources,

    /**
     * Effectively **per network**: a list of accounts, personas and connected dApps.
     */
    @SerialName("perNetwork")
    val perNetwork: List<PerNetwork>,

    /**
     * Version starting from 0.0.1
     */
    @SerialName("version")
    val version: String
) {
    fun toProfile(): Profile {
        return Profile(
            appPreferences = appPreferences,
            factorSources = factorSources,
            perNetwork = perNetwork,
            version = version
        )
    }
}
