package rdx.works.profile.domain.backup

import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.extensions.addMainBabylonDeviceFactorSource
import rdx.works.profile.data.model.extensions.changeGateway
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import java.time.Instant
import javax.inject.Inject

class RestoreAndCreateMainSeedPhraseUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val mnemonicRepository: MnemonicRepository
) {
    suspend operator fun invoke(
        backupType: BackupType
    ) {
        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)?.changeGateway(Radix.Gateway.mainnet)

        if (profile != null) {
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
        }
    }
}
