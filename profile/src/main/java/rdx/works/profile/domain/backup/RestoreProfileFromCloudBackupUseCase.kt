package rdx.works.profile.domain.backup

import rdx.works.core.InstantGenerator
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class RestoreProfileFromCloudBackupUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
) {

    suspend operator fun invoke() {
        val profile = backupProfileRepository.getRestoringProfileFromCloudBackup()

        if (profile != null) {
            val newDeviceName = deviceInfoRepository.getDeviceInfo().displayName
            val profileWithRestoredHeader = profile.copy(
                header = profile.header.claim(
                    deviceDescription = newDeviceName,
                    claimedDate = InstantGenerator()
                )
            )
            profileRepository.saveProfile(profileWithRestoredHeader)
        }
    }
}
