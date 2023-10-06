package rdx.works.profile.domain.account

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.renameAccountDisplayName
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class RenameAccountDisplayNameUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        accountToRename: Network.Account,
        newDisplayName: String
    ) {
        withContext(defaultDispatcher) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.renameAccountDisplayName(
                accountToRename = accountToRename,
                newDisplayName = newDisplayName
            )
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
