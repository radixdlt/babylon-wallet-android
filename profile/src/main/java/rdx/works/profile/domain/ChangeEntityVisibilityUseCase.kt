package rdx.works.profile.domain

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.changeAccountVisibility
import rdx.works.core.sargon.changePersonaVisibility
import rdx.works.core.sargon.unHideAllEntities
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class ChangeEntityVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun changeAccountVisibility(entityAddress: AccountAddress, hide: Boolean) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.changeAccountVisibility(accountAddress = entityAddress, hide = hide)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    suspend fun changePersonaVisibility(entityAddress: IdentityAddress, hide: Boolean) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.changePersonaVisibility(identityAddress = entityAddress, isHidden = hide)
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
