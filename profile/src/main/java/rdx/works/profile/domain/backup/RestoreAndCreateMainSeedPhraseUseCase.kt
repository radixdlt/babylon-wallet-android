package rdx.works.profile.domain.backup

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.NetworkId
import rdx.works.core.TimestampGenerator
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

class RestoreAndCreateMainSeedPhraseUseCase @Inject constructor(
    private val backupProfileRepository: BackupProfileRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val mnemonicRepository: MnemonicRepository
) {
    suspend operator fun invoke(
        backupType: BackupType
    ): Result<Unit> {
        // always restore backup on mainnet
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)

        if (profile != null) {
            val deviceInfo = deviceInfoRepository.getDeviceInfo()
            return mnemonicRepository.createNew().fold(onSuccess = { mnemonic ->
                val deviceFactorSource = FactorSource.Device.babylon(
                    mnemonicWithPassphrase = mnemonic,
                    model = deviceInfo.model,
                    name = deviceInfo.name,
                    createdAt = TimestampGenerator(),
                    isMain = true
                )

                val updatedProfile = profile.addMainBabylonDeviceFactorSource(
                    mainBabylonFactorSource = deviceFactorSource
                )
                profileRepository.saveProfile(updatedProfile)
                Result.success(Unit)
            }, onFailure = {
                Result.failure(ProfileException.SecureStorageAccess)
            })
        }
        return Result.failure(Exception("No profile to restore"))
    }
}
