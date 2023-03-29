package rdx.works.profile.domain.gateway

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.addGateway
import rdx.works.profile.data.model.apppreferences.changeGateway
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class GetGatewaysUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource
) {

    operator fun invoke() = profileDataSource
        .profile
        .filterNotNull()
        .map { profile ->
            profile.appPreferences.gateways
        }

}
