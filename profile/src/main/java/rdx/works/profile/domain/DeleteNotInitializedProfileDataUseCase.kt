package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeleteNotInitializedProfileDataUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke() {
        profileRepository.clearProfileDataOnly()
    }
}
