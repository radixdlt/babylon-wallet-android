package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import rdx.works.profile.data.model.ProfileSnapshot.Companion.MINIMUM
import rdx.works.profile.data.model.apppreferences.AppPreferences
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network

@Serializable
internal data class ProfileSnapshot(
    @SerialName("header")
    private val header: Header,

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
     * A list of accounts, personas and connected dApps.
     */
    @SerialName("networks")
    private val networks: List<Network>
) {

    fun toProfile(): Profile {
        return Profile(
            header = header,
            appPreferences = appPreferences,
            factorSources = factorSources,
            networks = networks
        )
    }

    companion object {
        /**
         * The minimum accepted snapshot version format. Lower versions are currently discarded.
         */
        const val MINIMUM = 34

        fun fromJson(serialised: String) = Json.decodeFromString<ProfileSnapshot>(serialised)
    }
}

@Serializable
internal data class ProfileSnapshotRelaxed(
    @SerialName("header")
    private val header: Header
) {

    val isValid: Boolean
        get() = header.snapshotVersion >= MINIMUM
}
