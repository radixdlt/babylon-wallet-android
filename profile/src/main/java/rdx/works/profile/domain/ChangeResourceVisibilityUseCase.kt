package rdx.works.profile.domain

import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.extensions.ResourceAppPreferences
import com.radixdlt.sargon.extensions.hideResource
import com.radixdlt.sargon.extensions.unhideResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.getResourcePreferences
import rdx.works.core.sargon.updateResourcePreferences
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class ChangeResourceVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun hide(resourceIdentifier: ResourceIdentifier) {
        return updateAssetPreferences { resourcePreferences ->
            resourcePreferences.hideResource(resourceIdentifier)
        }
    }

    suspend fun unhide(resourceIdentifier: ResourceIdentifier) {
        return updateAssetPreferences { resourcePreferences ->
            resourcePreferences.unhideResource(resourceIdentifier)
        }
    }

    private suspend fun updateAssetPreferences(operation: (ResourceAppPreferences) -> ResourceAppPreferences) {
        return withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedResourcePreferences = operation(profile.getResourcePreferences())
            val updatedProfile = profile.updateResourcePreferences(updatedResourcePreferences)
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
