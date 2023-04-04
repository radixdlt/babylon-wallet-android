package rdx.works.profile.domain.account

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.repository.MnemonicDataSource
import javax.inject.Inject

class GetAccountSignersUseCase @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val mnemonicDataSource: MnemonicDataSource
) {

    suspend operator fun invoke(networkId: Int, addresses: List<String>) =
        profileDataSource.profile.first().getAccountSigners(
            addresses = addresses,
            networkId = networkId,
            getMnemonic = { factorInstance ->
                mnemonicDataSource(factorInstance.id)
            }
        )
}
