package rdx.works.profile.domain.backup

import kotlinx.coroutines.flow.combine
import rdx.works.core.domain.cloudbackup.CloudBackupState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class GetCloudBackupStateUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager
) {

    operator fun invoke() = combine(
        profileRepository.profile,
        preferencesManager.lastCloudBackupInstant
    ) { profile, lastCloudBackupInstant ->
        val email = googleSignInManager.getSignedInGoogleAccount()?.email

        if (profile.canBackupToCloud && email != null) {
            CloudBackupState.Enabled(email = email)
        } else {
            CloudBackupState.Disabled(
                email = email,
                lastCloudBackupTime = lastCloudBackupInstant
            )
        }
    }

//    operator fun invoke() = combine(
//        profileRepository.profile,
//        preferencesManager.lastBackupInstant,
//        minuteUpdateFlow(),
//    ) { profile, lastBackupInstant, _ ->
//        if (profile.appPreferences.security.isCloudProfileSyncEnabled) {
//            BackupState.Open(
//                lastCloudBackupTime = lastBackupInstant,
//                lastProfileUpdate = profile.header.lastModified,
//                lastCheck = TimestampGenerator()
//            )
//        } else {
//            BackupState.Closed
//        }
//    }

//    private fun minuteUpdateFlow() = flow {
//        while (currentCoroutineContext().isActive) {
//            emit(Unit)
//            delay(MINUTE_MS)
//        }
//    }.flowOn(defaultDispatcher)

//    companion object {
//        private const val MINUTE_MS = 60_000L
//    }
}
