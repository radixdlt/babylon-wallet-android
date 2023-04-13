package rdx.works.profile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import rdx.works.profile.data.model.Profile.Companion.equals
import rdx.works.profile.data.model.serialisers.InstantSerializer
import java.time.Instant

@Serializable
data class Header(
    /**
     * A description of the device the Profile was first generated on,
     * typically the wallet app reads a human provided device name
     * if present and able, and/or a model description of the device e.g:
     * `"Galaxy A53 5G (Samsung SM-A536B)"`
     * This string can be presented to the user during a recovery flow,
     * when the profile is restored from backup.
     *
     * This string is as constructed from [DeviceInfo] will be formed firt by the user's generated
     * device name followed by the device's manufacturer and the device's factory model.
     */
    @SerialName("creatingDevice")
    val creatingDevice: String,

    /**
     * A locally generated stable identifier of this Profile. Useful for checking if
     * two [Profile]s which are unequal based on [equals] (content) might be
     * semantically the same, based on the ID.
     */
    @SerialName("id")
    val id: String,

    /**
     * When this profile was first created
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("creationDate")
    val creationDate: Instant,

    /**
     * When the profile was last updated, by modifications from the user
     */
    @Serializable(with = InstantSerializer::class)
    @SerialName("lastModified")
    val lastModified: Instant,

    /**
     * A version of the Profile Snapshot data format used for compatibility checks.
     */
    val snapshotVersion: Int
) {

    companion object {
        private const val GENERIC_ANDROID_DEVICE_PLACEHOLDER = "Android Phone"

        fun init(
            id: String,
            creatingDevice: String?,
            creationDate: Instant
        ) = Header(
            creatingDevice = if (creatingDevice.isNullOrBlank()) GENERIC_ANDROID_DEVICE_PLACEHOLDER else creatingDevice,
            id = id,
            creationDate = creationDate,
            lastModified = creationDate,
            snapshotVersion = ProfileSnapshot.MINIMUM
        )
    }

}
