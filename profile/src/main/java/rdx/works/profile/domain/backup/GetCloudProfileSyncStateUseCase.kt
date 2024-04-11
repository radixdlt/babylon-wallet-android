package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCloudProfileSyncStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    operator fun invoke() = profileRepository
        .profile
        .map {
            it.appPreferences.security.isCloudProfileSyncEnabled
        }
}
