package rdx.works.profile.domain.backup

import com.radixdlt.sargon.NetworkId
import kotlinx.coroutines.flow.firstOrNull
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.os.SargonOsManager
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.data.repository.BackupProfileRepository
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val checkMigrationToNewBackupSystemUseCase: CheckMigrationToNewBackupSystemUseCase,
    private val sargonOsManager: SargonOsManager
) {

    suspend operator fun invoke(
        backupType: BackupType,
        mainSeedPhraseSkipped: Boolean
    ): Result<Unit> {
        val sargonOs = sargonOsManager.sargonOs.firstOrNull() ?: return Result.failure(RuntimeException("Sargon os not booted"))

        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)
            ?.changeGatewayToNetworkId(NetworkId.MAINNET) ?: return Result.failure(RuntimeException("No restoring profile available"))

        return runCatching {
            sargonOs.importWallet(
                profile = profile,
                bdfsSkipped = mainSeedPhraseSkipped
            )
        }.onSuccess {
            backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
            if (backupType is BackupType.Cloud || backupType is BackupType.DeprecatedCloud) {
                checkMigrationToNewBackupSystemUseCase.revokeAccessToDeprecatedCloudBackup()
            }
        }
    }
}
