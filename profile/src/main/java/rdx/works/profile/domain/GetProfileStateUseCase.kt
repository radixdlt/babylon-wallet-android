package rdx.works.profile.domain

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class GetProfileStateUseCase @Inject constructor(private val dataSource: ProfileRepository) {

    operator fun invoke() = dataSource.profileState
}

/**
 * Checks the validity of the profile. A profile might have been temporarily generated, but might contain no accounts.
 * This is considered as a profile that is not properly initialized, as a correct profile should have at least one account
 */
suspend fun GetProfileStateUseCase.isInitialized(): Boolean = invoke().firstOrNull()?.let {
    it is ProfileState.Restored && it.hasMainnet()
} == true
