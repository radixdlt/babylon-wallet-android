package rdx.works.profile.domain

import rdx.works.profile.data.model.pernetwork.Entity

sealed class ProfileException : Exception() {
    data class AuthenticationSigningAlreadyExist(val entity: Entity) :
        Exception("Signing Entity $entity already has authenticationSigning")
}
