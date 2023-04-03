package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(private val dataSource: ProfileDataSource) {

    suspend operator fun invoke() = dataSource.clear()
}
