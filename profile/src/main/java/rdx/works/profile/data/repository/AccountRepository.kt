package rdx.works.profile.data.repository

import com.radixdlt.bip39.model.MnemonicWords
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.extensions.signerPrivateKey
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.domain.GetMnemonicUseCase
import javax.inject.Inject

interface AccountRepository {

    val accounts: Flow<List<Account>>

    suspend fun getAccounts(): List<Account>

    suspend fun getAccountByAddress(address: String): Account?

    suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner>
}

class AccountRepositoryImpl @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val getMnemonicUseCase: GetMnemonicUseCase
) : AccountRepository {

    override val accounts: Flow<List<Account>> = profileDataSource.profile
        .map { profile ->
            profile
                ?.perNetwork
                ?.firstOrNull { perNetwork ->
                    perNetwork.networkID == getCurrentNetwork().networkId().value
                }
        }.map { perNetwork ->
            perNetwork?.accounts.orEmpty()
        }

    override suspend fun getAccounts(): List<Account> {
        val perNetwork = getPerNetwork()
        return perNetwork?.accounts.orEmpty()
    }

    override suspend fun getAccountByAddress(address: String): Account? {
        val perNetwork = getPerNetwork()
        return perNetwork
            ?.accounts
            ?.firstOrNull { account ->
                account.entityAddress.address == address
            }
    }

    override suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner> {
        val profile = profileDataSource.readProfile()
        val accounts = getSignerAccountsForAddresses(profile, addresses, networkId)
        val factorSourceId = profile?.notaryFactorSource()?.factorSourceID
        assert(factorSourceId != null)
        val mnemonic = getMnemonicUseCase(factorSourceId)
        assert(mnemonic.isNotEmpty())
        val mnemonicWords = MnemonicWords(mnemonic)
        val signers = mutableListOf<AccountSigner>()
        accounts.forEach { account ->
            val privateKey = mnemonicWords.signerPrivateKey(derivationPath = account.derivationPath)
            signers.add(
                AccountSigner(
                    account = account,
                    privateKey = privateKey
                )
            )
        }
        return signers.toList()
    }

    private suspend fun getSignerAccountsForAddresses(
        profile: Profile?,
        addresses: List<String>,
        networkId: Int,
    ): List<Account> {
        val accounts = if (addresses.isNotEmpty()) {
            addresses.mapNotNull { address ->
                getAccountByAddress(address)
            }
        } else {
            listOfNotNull(
                profile?.perNetwork
                    ?.firstOrNull { perNetwork ->
                        perNetwork.networkID == networkId
                    }
                    ?.accounts
                    ?.first()
            )
        }
        return accounts
    }

    private suspend fun getPerNetwork(): PerNetwork? {
        return profileDataSource.readProfile()
            ?.perNetwork
            ?.firstOrNull { perNetwork ->
                perNetwork.networkID == getCurrentNetwork().networkId().value
            }
    }

    private suspend fun getCurrentNetwork(): Network {
        return profileDataSource.readProfile()
            ?.appPreferences
            ?.networkAndGateway?.network
            ?: NetworkAndGateway.nebunet.network
    }
}
