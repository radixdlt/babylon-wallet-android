package rdx.works.profile.domain

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.updateProfile
import java.time.Instant
import javax.inject.Inject

class EnsureBabylonFactorSourceExistUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository
) {

    suspend operator fun invoke(): Profile {
        return profileRepository.updateProfile { profile ->
            if (profile.babylonDeviceFactorSourceExist) {
                profile
            } else {
                val deviceInfo = deviceInfoRepository.getDeviceInfo()
                val mnemonic = mnemonicRepository()
                val factorSource = DeviceFactorSource.babylon(
                    mnemonicWithPassphrase = mnemonic,
                    model = deviceInfo.model,
                    name = deviceInfo.name,
                    createdAt = Instant.now()
                )
                profile.copy(factorSources = listOf(factorSource) + profile.factorSources)
            }
        }
    }
}
