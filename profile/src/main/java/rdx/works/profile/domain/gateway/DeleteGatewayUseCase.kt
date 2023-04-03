package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.deleteGateway
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class DeleteGatewayUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(gateway: Radix.Gateway) = profileDataSource
        .profile
        .first()
        .let { profile ->
            val updatedProfile = profile.deleteGateway(gateway)
            profileDataSource.saveProfile(updatedProfile)
        }
}
