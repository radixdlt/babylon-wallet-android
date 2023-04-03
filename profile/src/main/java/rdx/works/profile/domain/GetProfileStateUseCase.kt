package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class GetProfileStateUseCase @Inject constructor(private val dataSource: ProfileDataSource) {

    operator fun invoke() = dataSource.profileState
}

suspend fun GetProfileStateUseCase.exists() = invoke().first() is ProfileState.Restored
