package rdx.works.profile.domain.backup

import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import rdx.works.core.InstantGenerator
import rdx.works.core.TimestampGenerator
import rdx.works.core.mapError
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.core.then
import rdx.works.profile.cloudbackup.DriveClient
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.ProfileException
import timber.log.Timber
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val driveClient: DriveClient,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(
        backupType: BackupType,
        mainSeedPhraseSkipped: Boolean
    ): Result<Unit> {
        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)
            ?.changeGatewayToNetworkId(NetworkId.MAINNET) ?: return Result.failure(RuntimeException("No restoring profile available"))

        val newDevice = deviceInfoRepository.getDeviceInfo()
        val profileWithRestoredHeader = profile.copy(
            header = profile.header.copy(
                lastUsedOnDevice = DeviceInfo(
                    id = profile.header.id,
                    description = newDevice.displayName,
                    date = TimestampGenerator()
                )
            )
        )

        return if (mainSeedPhraseSkipped) {
            mnemonicRepository.createNew()
                .mapCatching { mnemonic ->
                    val deviceFactorSource = FactorSource.Device.babylon(
                        mnemonicWithPassphrase = mnemonic,
                        model = newDevice.model,
                        name = newDevice.name,
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
                Timber.tag("CloudBackup").d("Claiming Profile...")
                driveClient.claimCloudBackup(backupType.entity).onSuccess {
                    preferencesManager.setGoogleDriveFileId(it.id)
                    Timber.tag("CloudBackup").d("Save claimed profile")
                    profileRepository.saveProfile(profileToSave)
                    backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
                }.map {  }
            } else {
                Timber.tag("CloudBackup").d("Save profile")
                profileRepository.saveProfile(profileToSave)
                backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
                Result.success(Unit)
            }
        }
    }
}
