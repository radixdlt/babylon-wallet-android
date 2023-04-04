package rdx.works.profile.domain

import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class DeleteProfileUseCase @Inject constructor(private val dataSource: ProfileRepository) {

    suspend operator fun invoke() = dataSource.clear()
}
