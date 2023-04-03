package rdx.works.profile.data.model

sealed class ProfileState {

    object NotInitialised : ProfileState()

    object None : ProfileState()

    object Incompatible : ProfileState()

    data class Restored(val profile: Profile) : ProfileState()
}
