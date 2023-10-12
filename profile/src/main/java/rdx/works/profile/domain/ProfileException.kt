package rdx.works.profile.domain

import rdx.works.profile.data.model.pernetwork.Entity

sealed class ProfileException : Exception() {
    data class AuthenticationSigningAlreadyExist(val entity: Entity) :
        Exception("Signing Entity $entity already has authenticationSigning")
}

object InvalidSnapshotException : java.lang.Exception("The snapshot is invalid")
object InvalidPasswordException : java.lang.Exception("The password is invalid")
object NoMnemonicException : java.lang.Exception("Please restore your Seed Phrase and try again")
