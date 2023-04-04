package rdx.works.profile.data.repository

import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetMnemonicUseCase
import javax.inject.Inject

interface AccountRepository {

    suspend fun getAccounts(): List<Network.Account>

    suspend fun getAccountByAddress(address: String): Network.Account?

    suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner>
}

class AccountRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val getMnemonicUseCase: GetMnemonicUseCase
) : AccountRepository {

    override suspend fun getAccounts(): List<Network.Account> {
        val perNetwork = getPerNetwork()
        return perNetwork.accounts
    }

    override suspend fun getAccountByAddress(address: String): Network.Account? {
        val perNetwork = getPerNetwork()
        return perNetwork
            .accounts
            .firstOrNull { account ->
                account.address == address
            }
    }

    override suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner> {
        val profile = profileDataSource.profile.first()

        return profile.getAccountSigners(
            addresses = addresses,
            networkId = networkId,
            getMnemonic = { factorInstance ->
                getMnemonicUseCase(factorInstance.id)
            }
        )
    }

    private suspend fun getPerNetwork(): Network {
        return profileDataSource.profile.first().currentNetwork
    }
}
