package rdx.works.profile.domain

import com.radixdlt.sargon.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.domain.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileRepository: ProfileRepository) {

    val flow: Flow<Profile>
        get() = profileRepository.profile

    val state = profileRepository.profileState

    suspend operator fun invoke() = flow.first()

    /**
     * Checks the validity of the profile. A profile might have been temporarily generated, but might contain no accounts.
     * This is considered as a profile that is not properly initialized, as a correct profile should have at least one account,
     * meaning at least one network.
     *
     * This method is crucial for backing up the profile, since we don't want to sync profile to Drive when profile is
     * not initialized yet. For example, when user adds a ledger to create an account in onboarding flow.
     *
     */
    suspend fun isInitialized(): Boolean = profileRepository.profileState
        .firstOrNull()?.let { state ->
            isInitialized(state)
        } == true

    private fun isInitialized(state: ProfileState): Boolean {
        return state is ProfileState.Restored && state.hasNetworks()
    }
}
