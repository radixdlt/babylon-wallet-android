package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class UpdateDeveloperModeUseCase @Inject constructor(
    val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(isEnabled: Boolean) = withContext(defaultDispatcher) {
        profileDataSource.updateDeveloperMode(isEnabled = isEnabled)
    }
}
