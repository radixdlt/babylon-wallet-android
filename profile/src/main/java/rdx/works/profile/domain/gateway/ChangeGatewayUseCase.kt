package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class ChangeGatewayUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke(gateway: Radix.Gateway) = profileDataSource
        .profile
        .firstOrNull()
        ?.let { profile ->
            val updatedProfile = profile.changeGateway(gateway)
            profileDataSource.saveProfile(updatedProfile)
        }

}
