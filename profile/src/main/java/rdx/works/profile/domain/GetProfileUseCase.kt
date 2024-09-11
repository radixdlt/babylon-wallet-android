package rdx.works.profile.domain

import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ProfileState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import rdx.works.core.di.IoDispatcher
import rdx.works.core.sargon.hasNetworks
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    val flow: Flow<Profile>
        get() = profileRepository.profile.flowOn(dispatcher)

    val state = profileRepository.profileState.flowOn(dispatcher)

    suspend operator fun invoke() = withContext(dispatcher) {
        flow.first()
    }

    /**
     * Checks the validity of the profile. A profile might have been temporarily generated, but might contain no networks.
     * This is considered as a profile that is not properly initialized, as a correct profile should have at least one network.
     *
     * This method is crucial for backing up the profile, since we don't want to sync profile to Drive when profile is
     * not initialized yet. For example, when user adds a ledger to create an account in onboarding flow.
     */
    suspend fun finishedOnboardingProfile(): Profile? = profileRepository.profileState
        .map { state ->
            (state as? ProfileState.Loaded)?.v1?.takeIf { it.hasNetworks }
        }
        .firstOrNull()
}
