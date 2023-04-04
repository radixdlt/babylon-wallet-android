package rdx.works.profile.domain

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileDataSource: ProfileDataSource) {

    operator fun invoke() = profileDataSource.profile
}

val GetProfileUseCase.accountsOnCurrentNetwork
    get() = invoke()
        .map { it.currentNetwork.accounts }
        .distinctUntilChanged()

val GetProfileUseCase.gateways
    get() = invoke()
        .map { it.appPreferences.gateways }
        .distinctUntilChanged()

val GetProfileUseCase.p2pLinks
    get() = invoke()
        .map { it.appPreferences.p2pLinks }
        .distinctUntilChanged()
