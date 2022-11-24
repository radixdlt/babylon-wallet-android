package rdx.works.profile

import rdx.works.profile.model.apppreferences.NetworkAndGateway
import javax.inject.Inject

class GenerateProfileUseCase @Inject constructor(
    private val generateMnemonicUseCase: GenerateMnemonicUseCase
) {

    suspend operator fun invoke(
        displayName: String
    ): Profile {
        val mnemonic = generateMnemonicUseCase()

        val networkAndGateway = NetworkAndGateway.primary

        return Profile.init(
            networkAndGateway = networkAndGateway,
            mnemonic = mnemonic,
            firstAccountDisplayName = displayName
        )
    }
}
