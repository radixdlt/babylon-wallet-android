package rdx.works.profile.domain

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.MnemonicWithPassphrase
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.init
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.domain.ProfileState
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.addAccounts
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val preferencesManager: PreferencesManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): Profile {
        return when (val state = profileRepository.profileState.first()) {
            is ProfileState.Restored -> state.profile
            else -> withContext(defaultDispatcher) {
                error("Integration")
//                val profile = Profile.init(
//                    id = UUIDGenerator.uuid().toString(),
//                    deviceInfo = deviceInfoRepository.getDeviceInfo(),
//                    creationDate = InstantGenerator()
//                )
//
//                profileRepository.saveProfile(profile)
//
//                profile
            }
        }
    }

    suspend fun initWithDeviceFactorSourceAndAccounts(
        deviceFactorSource: FactorSource.Device,
        mnemonicWithPassphrase: MnemonicWithPassphrase,
        accounts: List<Account>
    ): Profile {
        val networkId = accounts.firstOrNull()?.networkId ?: NetworkId.MAINNET
        return when (val state = profileRepository.profileState.first()) {
            is ProfileState.Restored -> state.profile
            else -> withContext(defaultDispatcher) {
                val profile = Profile.init(
                    deviceFactorSource = deviceFactorSource,
                    creatingDeviceName = deviceInfoRepository.getDeviceInfo().displayName
                ).addAccounts(
                    accounts = accounts,
                    onNetwork = networkId
                )
                profileRepository.saveProfile(profile)
                mnemonicRepository.saveMnemonic(deviceFactorSource.value.id.asGeneral(), mnemonicWithPassphrase)
                preferencesManager.markFactorSourceBackedUp(deviceFactorSource.value.id.asGeneral())

                profile
            }
        }
    }
}
