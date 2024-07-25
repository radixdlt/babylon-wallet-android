package rdx.works.profile.domain.backup

import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.from
import rdx.works.core.TimestampGenerator
import rdx.works.core.mapError
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.sargon.claim
import rdx.works.core.then
import rdx.works.core.toUnitResult
import rdx.works.profile.cloudbackup.data.DriveClient
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.HostInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.ProfileException
import timber.log.Timber
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val hostInfoRepository: HostInfoRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val driveClient: DriveClient,
    private val checkMigrationToNewBackupSystemUseCase: CheckMigrationToNewBackupSystemUseCase
) {

    suspend operator fun invoke(
        backupType: BackupType,
        mainSeedPhraseSkipped: Boolean
    ): Result<Unit> {
        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)
            ?.changeGatewayToNetworkId(NetworkId.MAINNET) ?: return Result.failure(RuntimeException("No restoring profile available"))

        val hostId = hostInfoRepository.getHostId()
        val hostInfo = hostInfoRepository.getHostInfo()
        val profileWithRestoredHeader = profile.claim(deviceInfo = DeviceInfo.from(hostId, hostInfo))

        return if (mainSeedPhraseSkipped) {
            mnemonicRepository.createNew()
                .mapCatching { mnemonic ->
                    val deviceFactorSource = FactorSource.Device.babylon(
                        mnemonicWithPassphrase = mnemonic,
                        hostInfo = hostInfo,
                        createdAt = TimestampGenerator(),
                        isMain = true
                    )
                    profileWithRestoredHeader.addMainBabylonDeviceFactorSource(mainBabylonFactorSource = deviceFactorSource)
                }
                .mapError { ProfileException.SecureStorageAccess }
        } else {
            Result.success(profileWithRestoredHeader)
        }.then { profileToSave ->
            if (backupType is BackupType.Cloud) {
                Timber.tag("CloudBackup").d("☁\uFE0F Claiming Profile...")
                driveClient.claimCloudBackup(
                    file = backupType.entity,
                    claimingProfile = profileToSave
                ).onSuccess {
                    Timber.tag("CloudBackup").d("☁\uFE0F Profile claimed, and now save it")
                    profileRepository.saveProfile(profileToSave)
                    backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
                    checkMigrationToNewBackupSystemUseCase.revokeAccessToDeprecatedCloudBackup()
                }.toUnitResult()
            } else {
                if (backupType is BackupType.DeprecatedCloud) {
                    checkMigrationToNewBackupSystemUseCase.revokeAccessToDeprecatedCloudBackup()
                }

                Timber.tag("CloudBackup").d("Save profile")
                profileRepository.saveProfile(profileToSave)
                backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
                Result.success(Unit)
            }
        }
    }
}
