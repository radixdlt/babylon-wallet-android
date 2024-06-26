package rdx.works.profile.cloudbackup.domain

import android.app.backup.BackupManager
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.canBackupToCloud
import rdx.works.profile.cloudbackup.data.GoogleSignInManager
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class CheckMigrationToNewBackupSystemUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val googleSignInManager: GoogleSignInManager,
    private val oldBackupManager: BackupManager,
) {

    suspend operator fun invoke(): Boolean {
        val profile = profileRepository.profile.firstOrNull() ?: return false

        return profile.canBackupToCloud && preferencesManager.isUsingDeprecatedCloudBackup() && !googleSignInManager.isSignedIn()
    }

    suspend fun revokeAccessToDeprecatedCloudBackup() {
        preferencesManager.clearDeprecatedCloudBackupIndicator()
        oldBackupManager.dataChanged() // Notify old backup manager to delete data
    }
}
