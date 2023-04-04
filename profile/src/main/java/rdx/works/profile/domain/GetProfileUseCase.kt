package rdx.works.profile.domain

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileDataSource: ProfileDataSource) {

    operator fun invoke() = profileDataSource.profile
}

fun GetProfileUseCase.accountsOnCurrentNetwork() = invoke()
    .map { it.currentNetwork.accounts }
    .distinctUntilChanged()

fun GetProfileUseCase.gateways() = invoke()
    .map { it.appPreferences.gateways }
    .distinctUntilChanged()

fun GetProfileUseCase.p2pLinks() = invoke()
    .map { it.appPreferences.p2pLinks }
    .distinctUntilChanged()
