@file:Suppress("TooManyFunctions")

package rdx.works.profile.domain

import com.radixdlt.sargon.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileRepository: ProfileRepository) {

    val flow: Flow<Profile>
        get() = profileRepository.profile

    suspend operator fun invoke() = flow.first()

    suspend fun isInitialized(): Boolean {
        return when (val profileState = profileRepository.profileState.first()) {
            ProfileState.NotInitialised,
            ProfileState.None -> false

            ProfileState.Incompatible -> true
            is ProfileState.Restored -> profileState.hasMainnet()
        }
    }
}
