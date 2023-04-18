package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.combine
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager
) {

    operator fun invoke() = combine(
        profileRepository.profile,
        preferencesManager.lastBackupInstant
    ) { profile, lastBackupInstant ->
        BackupState.from(profile, lastBackupInstant)
    }

}
