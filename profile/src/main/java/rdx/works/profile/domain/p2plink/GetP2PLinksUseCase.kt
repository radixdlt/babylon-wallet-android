package rdx.works.profile.domain.p2plink

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

class GetP2PLinksUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource,
) {

    operator fun invoke() = profileDataSource
        .profile
        .map { profile ->
            profile.appPreferences.p2pLinks
        }
        .distinctUntilChanged()
}
