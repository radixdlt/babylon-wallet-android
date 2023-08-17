package rdx.works.profile.domain.backup

import rdx.works.core.InstantGenerator
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class RestoreProfileFromBackupUseCase @Inject constructor(
    private val repository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
) {

    suspend operator fun invoke(): Profile? {
        val profile = repository.getRestoredProfileFromBackup()

        if (profile != null) {
            val newDeviceName = deviceInfoRepository.getDeviceInfo().displayName
            val profileWithRestoredHeader = profile.copy(
                header = profile.header.claim(
                    deviceDescription = newDeviceName,
                    claimedDate = InstantGenerator()
                )
            )
            repository.saveProfile(profileWithRestoredHeader)
        }

        return profile
    }
}
