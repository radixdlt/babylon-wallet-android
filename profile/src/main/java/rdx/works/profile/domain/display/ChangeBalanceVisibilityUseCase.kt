package rdx.works.profile.domain.display

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.extensions.changeBalanceVisibility
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.IoDispatcher
import javax.inject.Inject

class ChangeBalanceVisibilityUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(isVisible: Boolean) {
        withContext(ioDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.changeBalanceVisibility(isVisible = isVisible)
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
