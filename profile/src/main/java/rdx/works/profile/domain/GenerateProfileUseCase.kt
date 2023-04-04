package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileDataSource: ProfileDataSource,
    private val deviceInfoRepository: DeviceInfoRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(accountDisplayName: String): Profile {
        return when (val state = profileDataSource.profileState.first()) {
            is ProfileState.Restored -> state.profile
            else -> withContext(defaultDispatcher) {
                val mnemonicWithPassphrase = mnemonicRepository()

                val profile = Profile.init(
                    mnemonicWithPassphrase = mnemonicWithPassphrase,
                    firstAccountDisplayName = accountDisplayName,
                    creatingDevice = deviceInfoRepository.getDeviceInfo().displayName
                )

                profileDataSource.saveProfile(profile)

                profile
            }
        }
    }
}
