package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
    private val profileRepository: ProfileRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(accountDisplayName: String): Profile {
        profileRepository.readProfile()?.let { profile ->
            return profile
        } ?: run {
            return withContext(defaultDispatcher) {
                val mnemonic = getMnemonicUseCase()

                val networkAndGateway = NetworkAndGateway.nebunet

                val profile = Profile.init(
                    networkAndGateway = networkAndGateway,
                    mnemonic = MnemonicWords(phrase = mnemonic),
                    firstAccountDisplayName = accountDisplayName
                )

                profileRepository.saveProfile(profile)

                profile
            }
        }
    }
}
