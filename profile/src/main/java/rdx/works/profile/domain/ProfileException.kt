package rdx.works.profile.domain

import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.string

sealed class ProfileException(msg: String? = null, cause: Throwable? = null) : Throwable(msg, cause) {
    data class AuthenticationSigningAlreadyExist(val entity: ProfileEntity) :
        ProfileException("Signing Entity ${entity.address.string} already has authenticationSigning")

    data object InvalidSnapshot : ProfileException("The snapshot is invalid")
    data object InvalidPassword : ProfileException("The password is invalid")
    data object NoMnemonic : ProfileException("Please restore your Seed Phrase and try again")

    data class BdfsSecureStorage(val isSamsungDevice: Boolean) : ProfileException("There was issue tying to save BDFS for your profile")
}
