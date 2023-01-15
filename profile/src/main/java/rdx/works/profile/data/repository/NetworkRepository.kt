package rdx.works.profile.data.repository

import com.radixdlt.bip39.model.MnemonicWords
import rdx.works.profile.data.extensions.setNetworkAndGateway
import rdx.works.profile.data.extensions.signerPrivateKey
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Network
import rdx.works.profile.data.model.apppreferences.NetworkAndGateway
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetMnemonicUseCase
import javax.inject.Inject

interface NetworkRepository {

    suspend fun getCurrentNetworkId(): NetworkId

    suspend fun getCurrentNetworkBaseUrl(): String

    suspend fun hasAccountOnNetwork(
        newUrl: String,
        networkName: String
    ): Boolean

    suspend fun setNetworkAndGateway(
        newUrl: String,
        networkName: String
    )

    suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner>
}

class NetworkRepositoryImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getMnemonicUseCase: GetMnemonicUseCase
) : NetworkRepository {

    override suspend fun getCurrentNetworkId(): NetworkId {
        return getNetworkAndGateway().network.networkId()
    }

    override suspend fun getCurrentNetworkBaseUrl(): String {
        return getNetworkAndGateway().gatewayAPIEndpointURL
    }

    override suspend fun hasAccountOnNetwork(
        newUrl: String,
        networkName: String
    ): Boolean {
        val knownNetwork = Network
            .allKnownNetworks()
            .firstOrNull { network ->
                network.name == networkName
            } ?: return false

        val newNetworkAndGateway = NetworkAndGateway(
            gatewayAPIEndpointURL = newUrl,
            network = knownNetwork
        )

        return profileRepository.readProfile()?.let { profile ->
            profile.perNetwork.any { perNetwork ->
                perNetwork.networkID == newNetworkAndGateway.network.id
            }
        } ?: false
    }

    override suspend fun setNetworkAndGateway(
        newUrl: String,
        networkName: String
    ) {
        profileRepository.readProfile()?.let { profile ->
            val updatedProfile = profile.setNetworkAndGateway(
                NetworkAndGateway(
                    gatewayAPIEndpointURL = newUrl,
                    network = Network.allKnownNetworks()
                        .first { network ->
                            network.name == networkName
                        }
                )
            )
            profileRepository.saveProfile(updatedProfile)
        }
    }

    override suspend fun getSignersForAddresses(
        networkId: Int,
        addresses: List<String>
    ): List<AccountSigner> {
        val profile = profileRepository.readProfile()
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

    private suspend fun getNetworkAndGateway(): NetworkAndGateway {
        return profileRepository.readProfile()
            ?.appPreferences
            ?.networkAndGateway
            ?: NetworkAndGateway.nebunet
    }

    private suspend fun getSignerAccountsForAddresses(
        profile: Profile?,
        addresses: List<String>,
        networkId: Int,
    ): List<Account> {
        val accounts = if (addresses.isNotEmpty()) {
            addresses.mapNotNull { address ->
                profileRepository.getAccount(address)
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
}
