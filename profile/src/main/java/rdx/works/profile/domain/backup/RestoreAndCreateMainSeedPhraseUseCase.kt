package rdx.works.profile.domain.backup

import com.radixdlt.sargon.DeviceFactorSource
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.asGeneral
import rdx.works.core.TimestampGenerator
import rdx.works.core.sargon.addMainBabylonDeviceFactorSource
import rdx.works.core.sargon.babylon
import rdx.works.core.sargon.changeGatewayToNetworkId
import rdx.works.profile.data.repository.BackupProfileRepository
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
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
        val profile = backupProfileRepository.getTemporaryRestoringProfile(backupType)?.changeGatewayToNetworkId(NetworkId.MAINNET)

        if (profile != null) {
            val deviceInfo = deviceInfoRepository.getDeviceInfo()
            val mnemonic = mnemonicRepository()
            val deviceFactorSource = DeviceFactorSource.babylon(
                mnemonicWithPassphrase = mnemonic,
                model = deviceInfo.model,
                name = deviceInfo.name,
                createdAt = TimestampGenerator(),
                isMain = true
            )

            val updatedProfile = profile.addMainBabylonDeviceFactorSource(mainBabylonFactorSource = deviceFactorSource.asGeneral())
            profileRepository.saveProfile(updatedProfile)
        }
    }
}
