package rdx.works.profile.domain.backup

import android.app.backup.BackupDataOutput
import rdx.works.core.InstantGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class BackupProfileToCloudUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(data: BackupDataOutput?, tag: String): Result<Unit> {
        val snapshot = backupProfileRepository.getSnapshotForBackup(BackupType.Cloud) ?: return Result.failure(
            Exception("Snapshot does not exist")
        )

        data?.apply {
            val byteArray = snapshot.toByteArray(Charsets.UTF_8)
            val len: Int = byteArray.size

            writeEntityHeader(tag, len)
            writeEntityData(byteArray, len)
            preferencesManager.updateLastBackupInstant(InstantGenerator())
            backupProfileRepository.saveTemporaryRestoringSnapshot(snapshot, BackupType.Cloud)
        }

        return Result.success(Unit)
    }
}
