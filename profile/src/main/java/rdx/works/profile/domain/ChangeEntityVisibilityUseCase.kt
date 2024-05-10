package rdx.works.profile.domain

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.hideAccount
import rdx.works.core.sargon.hidePersona
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeEntityVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun hideAccount(entityAddress: AccountAddress) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.hideAccount(accountAddress = entityAddress)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    suspend fun hidePersona(entityAddress: IdentityAddress) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.hidePersona(identityAddress = entityAddress)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    suspend fun unHideAll() {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.unHideAllEntities()
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
