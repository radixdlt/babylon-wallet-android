package rdx.works.profile.data.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Header.Companion.equals
import rdx.works.profile.data.model.Profile.Companion.equals
import rdx.works.profile.data.model.ProfileSnapshot.Companion.MINIMUM
import java.time.Instant

@Serializable
data class Header(
    @SerialName("creatingDevice")
    val creatingDevice: Device,

    @SerialName("lastUsedOnDevice")
    val lastUsedOnDevice: Device,

    /**
     * A locally generated stable identifier of this Profile. Useful for checking if
     * two [Profile]s which are unequal based on [equals] (content) might be
     * semantically the same, based on the ID.
     */
    @SerialName("id")
    val id: String,

    /**
     * When the profile was last updated, by modifications from the user
     */
    @SerialName("lastModified")
    @Contextual
    val lastModified: Instant,

    /**
     * A version of the Profile Snapshot data format used for compatibility checks.
     */
    @SerialName("snapshotVersion")
    val snapshotVersion: Int,

    /**
     * General information about the total networks, accounts and personas
     */
    @SerialName("contentHint")
    val contentHint: ContentHint
) {

    /**
     * When this profile was first created
     */
    val creationDate: Instant
        get() = creatingDevice.date

    val isCompatible: Boolean
        get() = snapshotVersion >= MINIMUM

    companion object {
        private const val GENERIC_ANDROID_DEVICE_PLACEHOLDER = "Android Phone"

        @Suppress("LongParameterList")
        fun init(
            id: String,
            deviceName: String?,
            creationDate: Instant,
            numberOfNetworks: Int,
            numberOfAccounts: Int = 0,
            numberOfPersonas: Int = 0,
        ): Header {
            val device = Device(
                description = if (deviceName.isNullOrBlank()) GENERIC_ANDROID_DEVICE_PLACEHOLDER else deviceName,
                id = id,
                date = creationDate
            )

            return Header(
                creatingDevice = device,
                lastUsedOnDevice = device,
                id = id,
                lastModified = creationDate,
                snapshotVersion = ProfileSnapshot.MINIMUM,
                contentHint = ContentHint(
                    numberOfNetworks = numberOfNetworks,
                    numberOfAccountsOnAllNetworksInTotal = numberOfAccounts,
                    numberOfPersonasOnAllNetworksInTotal = numberOfPersonas
                )
            )
        }
    }

    fun claim(deviceDescription: String, claimedDate: Instant): Header = copy(
        lastUsedOnDevice = Device(
            description = deviceDescription,
            id = id,
            date = claimedDate
        )
    )

    @Serializable
    data class Device(
        /**
         * A description of the [Device] the Profile was generated/claimed,
         * typically the wallet app reads a human provided device name
         * if present and able, and/or a model description of the device e.g:
         * `"Galaxy A53 5G (Samsung SM-A536B)"`
         * This string can be presented to the user during a recovery flow,
         * when the profile is restored from backup.
         *
         * This string is as constructed from [DeviceInfo] will be formed first by the user's generated
         * device name followed by the device's manufacturer and the device's factory model.
         */
        @SerialName("description")
        val description: String,

        /**
         * The profile's id which is associated with this [Device]
         */
        @SerialName("id")
        val id: String,

        /**
         * The [Instant] on which this profile was generated/claimed
         */
        @SerialName("date")
        @Contextual
        val date: Instant
    )

    @Serializable
    data class ContentHint(
        @SerialName("numberOfAccountsOnAllNetworksInTotal")
        val numberOfAccountsOnAllNetworksInTotal: Int,
        @SerialName("numberOfPersonasOnAllNetworksInTotal")
        val numberOfPersonasOnAllNetworksInTotal: Int,
        @SerialName("numberOfNetworks")
        val numberOfNetworks: Int
    )
}
