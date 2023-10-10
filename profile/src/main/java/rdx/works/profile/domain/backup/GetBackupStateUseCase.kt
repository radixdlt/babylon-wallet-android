package rdx.works.profile.domain.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GetBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    operator fun invoke() = combine(
        profileRepository.profile,
        preferencesManager.lastBackupInstant,
        minuteUpdateFlow(),
    ) { profile, lastBackupInstant, _ ->
        if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
            BackupState.Open(
                lastCloudBackupTime = lastBackupInstant,
                lastProfileUpdate = profile.header.lastModified,
                lastCheck = InstantGenerator()
            )
        } else {
            BackupState.Closed
        }
    }

    private fun minuteUpdateFlow() = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(MINUTE_MS)
        }
    }.flowOn(defaultDispatcher)

    companion object {
        private const val MINUTE_MS = 60_000L
    }
}
