package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.firstOrNull
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.addGateway
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class GetCurrentGatewayUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    suspend operator fun invoke() = profileDataSource
        .profile
        .firstOrNull()?.appPreferences?.gateways?.current() ?: Radix.Gateway.default

}
