package rdx.works.profile.domain

import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val generateMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(
        displayName: String
    ): Profile {
        val mnemonic = generateMnemonicUseCase()

        val networkAndGateway = NetworkAndGateway.hammunet

        val profile = Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = displayName
        )

        val readProfile = profileRepository.readProfile()

        println("Profile $readProfile")

        profileRepository.saveProfile(profile)

        return profile
    }
}
