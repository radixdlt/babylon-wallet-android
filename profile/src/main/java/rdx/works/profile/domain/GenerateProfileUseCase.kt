package rdx.works.profile.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rdx.works.core.InstantGenerator
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.DeviceInfoRepository
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val mnemonicRepository: MnemonicRepository,
    private val profileRepository: ProfileRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(): Profile {
        return when (val state = profileRepository.profileState.first()) {
            is ProfileState.Restored -> state.profile
            else -> withContext(defaultDispatcher) {
                val mnemonicWithPassphrase = mnemonicRepository()

                val profile = Profile.init(
                    mnemonicWithPassphrase = mnemonicWithPassphrase,
                    id = UUIDGenerator.uuid().toString(),
                    creatingDevice = deviceInfoRepository.getDeviceInfo().displayName,
                    creationDate = InstantGenerator()
                )

                profileRepository.saveProfile(profile)

                profile
            }
        }
    }
}
