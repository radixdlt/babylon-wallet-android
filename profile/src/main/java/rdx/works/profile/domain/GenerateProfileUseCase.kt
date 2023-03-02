package rdx.works.profile.domain

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Gateway
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.di.coroutines.DefaultDispatcher
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val getMnemonicUseCase: GetMnemonicUseCase,
    private val profileDataSource: ProfileDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(accountDisplayName: String): Profile {
        profileDataSource.readProfile()?.let { profile ->
            return profile
        } ?: run {
            return withContext(defaultDispatcher) {
                val mnemonic = getMnemonicUseCase()

                val gateway = Gateway.nebunet

                val profile = Profile.init(
                    gateway = gateway,
                    mnemonic = MnemonicWords(phrase = mnemonic),
                    firstAccountDisplayName = accountDisplayName
                )

                profileDataSource.saveProfile(profile)

                profile
            }
        }
    }
}
