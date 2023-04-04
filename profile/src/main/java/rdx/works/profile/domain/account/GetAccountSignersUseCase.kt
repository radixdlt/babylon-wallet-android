package rdx.works.profile.domain.account

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.MnemonicRepository
import javax.inject.Inject

class GetAccountSignersUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val mnemonicRepository: MnemonicRepository
) {

    suspend operator fun invoke(networkId: Int, addresses: List<String>) =
        profileRepository.profile.first().getAccountSigners(
            addresses = addresses,
            networkId = networkId,
            getMnemonic = { factorInstance ->
                mnemonicRepository(factorInstance.id)
            }
        )
}
