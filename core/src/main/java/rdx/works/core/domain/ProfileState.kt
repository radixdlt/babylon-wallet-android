package rdx.works.core.domain

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.invoke

sealed class ProfileState {

    /**
     * This is the initial value of the [ProfileState]. It is used to initialize the flow
     * that describes the saved state of the [ProfileSnapshot]. This is needed until the
     * repository owning this flow, initialises and reads it from internal storage.
     */
    data object NotInitialised : ProfileState()

    /**
     * The repository has tried to query the [ProfileSnapshot] from internal storage, but
     * there is nothing there. The user has not created a [Profile] yet.
     */
    data object None : ProfileState()

    /**
     * The [Profile]'s version saved in the internal storage is lower than the
     * [ProfileSnapshotVersion.V100], so it is incompatible. Currently the user can only
     * create a new profile.
     */
    data object Incompatible : ProfileState()

    /**
     * A compatible [ProfileSnapshot] exists and the user can derive the [Profile].
     */
    data class Restored(val profile: Profile) : ProfileState() {
        fun hasNetworks(): Boolean {
            return profile.networks().isNotEmpty()
        }
    }
}
