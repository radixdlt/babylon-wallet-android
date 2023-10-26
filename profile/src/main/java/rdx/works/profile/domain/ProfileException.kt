package rdx.works.profile.domain

import rdx.works.profile.data.model.pernetwork.Entity

sealed class ProfileException(msg: String? = null, cause: Throwable? = null) : Throwable(msg, cause) {
    data class AuthenticationSigningAlreadyExist(val entity: Entity) :
        ProfileException("Signing Entity $entity already has authenticationSigning")

    data object InvalidSnapshot : ProfileException("The snapshot is invalid")
    data object InvalidPassword : ProfileException("The password is invalid")
    data object NoMnemonic : ProfileException("Please restore your Seed Phrase and try again")
}