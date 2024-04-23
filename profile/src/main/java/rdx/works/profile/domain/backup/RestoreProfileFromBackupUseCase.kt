package rdx.works.profile.domain.backup

import com.radixdlt.sargon.DeviceInfo
import com.radixdlt.sargon.NetworkId
import rdx.works.core.InstantGenerator
import rdx.works.core.TimestampGenerator
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend operator fun invoke(backupType: BackupType) {
        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)

        if (profile != null) {
            val newDeviceName = deviceInfoRepository.getDeviceInfo().displayName
            val profileWithRestoredHeader = profile.copy(
                header = profile.header.copy(
                    lastUsedOnDevice = DeviceInfo(
                        id = profile.header.id, // TODO integration
                        description = newDeviceName,
                        date = TimestampGenerator()
                    )
                )
            )
            profileRepository.saveProfile(profileWithRestoredHeader)

            if (backupType is BackupType.Cloud) {
                preferencesManager.updateLastBackupInstant(InstantGenerator())
            } else {
                // Remove only temporary file backups, cloud backups can only be deleted
                // if user opts to create a new profile.
                backupProfileRepository.discardTemporaryRestoringSnapshot(backupType)
            }
        }
    }
}
