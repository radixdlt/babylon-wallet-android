package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
    private val profileDataSource: ProfileDataSource,
    private val deviceInfoRepository: DeviceInfoRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(accountDisplayName: String): Profile {
        profileDataSource.readProfile()?.let { profile ->
            return profile
        } ?: run {
            return withContext(defaultDispatcher) {
                val mnemonicWithPassphrase = getMnemonicUseCase()

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
