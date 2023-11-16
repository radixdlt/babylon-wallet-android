package rdx.works.profile.domain

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.extensions.addMainBabylonDeviceFactorSource
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import java.time.Instant
import javax.inject.Inject

class EnsureBabylonFactorSourceExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository
) {

    suspend operator fun invoke(): Profile {
        val profile = profileRepository.profile.first()
        if (profile.babylonDeviceFactorSourceExist) return profile
        val deviceInfo = deviceInfoRepository.getDeviceInfo()
        val mnemonic = mnemonicRepository()
        val deviceFactorSource = DeviceFactorSource.babylon(
            mnemonicWithPassphrase = mnemonic,
            model = deviceInfo.model,
            name = deviceInfo.name,
            createdAt = Instant.now(),
            isMain = true
        )
        val updatedProfile = profile.addMainBabylonDeviceFactorSource(
            mainBabylonFactorSource = deviceFactorSource
        )
        profileRepository.saveProfile(updatedProfile)
        return updatedProfile
    }

    suspend fun babylonFactorSourceExist(): Boolean {
        return profileRepository.profile.first().babylonDeviceFactorSourceExist
    }
}
