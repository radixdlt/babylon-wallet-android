package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.hideAccount
import rdx.works.profile.data.model.pernetwork.hidePersona
import rdx.works.profile.data.model.pernetwork.unhideAllEntities
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeEntityVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun hideAccount(entityAddress: String) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.hideAccount(address = entityAddress)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    suspend fun hidePersona(entityAddress: String) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.hidePersona(address = entityAddress)
            profileRepository.saveProfile(updatedProfile)
        }
    }

    suspend fun unhideAll() {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.unhideAllEntities()
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
