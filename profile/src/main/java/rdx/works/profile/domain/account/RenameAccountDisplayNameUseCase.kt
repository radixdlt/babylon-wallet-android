package rdx.works.profile.domain.account

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DisplayName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.sargon.renameAccountDisplayName
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.core.di.DefaultDispatcher
import javax.inject.Inject

class RenameAccountDisplayNameUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        accountToRename: Account,
        newDisplayName: DisplayName
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
