package rdx.works.profile.domain

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class ReadProfileUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(): Profile {
        profileRepository.readProfile()?.let { profile ->
            return profile
        } ?: run {
            val mnemonic = generateMnemonicUseCase()
            val networkAndGateway = NetworkAndGateway.hammunet

            val profile = Profile.init(
                networkAndGateway = networkAndGateway,
                mnemonic = mnemonic
            )
            println("Profile $profile")

            return profile
        }
    }
}
