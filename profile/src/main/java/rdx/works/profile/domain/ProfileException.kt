package rdx.works.profile.domain

import rdx.works.profile.data.model.pernetwork.SigningEntity

sealed class ProfileException : Exception() {
    data class AuthenticationSigningAlreadyExist(val signingEntity: SigningEntity) :
        Exception("Signing Entity $signingEntity already has authenticationSigning")
}
