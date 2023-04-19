package rdx.works.profile.domain.backup

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
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
        preferencesManager.lastBackupInstant,
        minuteUpdateFlow(),
    ) { profile, lastBackupInstant, _ ->
        BackupState.from(profile, lastBackupInstant)
    }

    private fun minuteUpdateFlow() = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(MINUTE_MS)
        }
    }

    companion object {
        private const val MINUTE_MS = 60_000L
    }
}
