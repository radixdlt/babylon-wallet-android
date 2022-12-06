package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository
) {

    suspend operator fun invoke(): Profile {
        profileRepository.readProfileSnapshot()?.let { profileSnapshot ->
            return profileSnapshot.toProfile()
        } ?: run {
            val mnemonic = getMnemonicUseCase()

            val networkAndGateway = NetworkAndGateway.hammunet

            val profile = Profile.init(
                networkAndGateway = networkAndGateway,
                mnemonic = MnemonicWords(phrase = mnemonic)
            )

            profileRepository.saveProfileSnapshot(profile.snapshot())

            return profile
        }
    }
}
