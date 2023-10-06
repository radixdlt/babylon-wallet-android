package rdx.works.profile.domain

import com.radixdlt.ret.Address
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.core.PUBLIC_KEY_HASH_LENGTH
import rdx.works.core.toByteArray
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileRepository: ProfileRepository) {
    operator fun invoke() = profileRepository.profile

    suspend fun isInitialized(): Boolean {
        val profileState = profileRepository.profileState.first()
        return profileState != ProfileState.NotInitialised && profileState != ProfileState.None
    }
}

/**
 * Accounts on network
 */
val GetProfileUseCase.accountsOnCurrentNetwork
    get() = invoke().map { it.currentNetwork.accounts }

val GetProfileUseCase.factorSources
    get() = invoke().map { profile -> profile.factorSources }

val GetProfileUseCase.deviceFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<DeviceFactorSource>() }

val GetProfileUseCase.deviceFactorSourcesWithAccounts
    get() = invoke().map { profile ->
        val deviceFactorSources = profile.factorSources.filterIsInstance<DeviceFactorSource>()

        val factorSourcesWithAccounts = mutableMapOf<DeviceFactorSource, MutableList<Network.Account>>()
        profile.currentNetwork.accounts.forEach { account ->
            val deviceFactorSource = deviceFactorSources.find { it.id == account.factorSourceId() }
            if (deviceFactorSource != null) {
                val accounts = factorSourcesWithAccounts.getOrPut(deviceFactorSource) { mutableListOf() }
                accounts.add(account)
            }
        }

        factorSourcesWithAccounts.mapValues { it.value.toList() }
    }

val GetProfileUseCase.ledgerFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<LedgerHardwareWalletFactorSource>() }

suspend fun GetProfileUseCase.factorSourceById(
    id: FactorSource.FactorSourceID
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.id == id
}

suspend fun GetProfileUseCase.accountsOnCurrentNetwork() = accountsOnCurrentNetwork.first()

suspend fun GetProfileUseCase.accountOnCurrentNetwork(
    withAddress: String
) = accountsOnCurrentNetwork().firstOrNull { account ->
    account.address == withAddress
}

suspend fun GetProfileUseCase.nextDerivationPathForAccountOnNetwork(networkId: Int): DerivationPath {
    val profile = invoke().first()
    val network = requireNotNull(NetworkId.from(networkId))
    return DerivationPath.forAccount(
        networkId = network,
        accountIndex = profile.nextAccountIndex(network),
        keyType = KeyType.TRANSACTION_SIGNING
    )
}

fun GetProfileUseCase.accountOnCurrentNetworkWithAddress(
    address: String
) = accountsOnCurrentNetwork.map { accounts ->
    accounts.firstOrNull { account ->
        account.address == address
    }
}

suspend fun GetProfileUseCase.currentNetworkAccountHashes(): Set<ByteArray> {
    return accountsOnCurrentNetwork().map {
        val addressData = Address(it.address).bytes().toByteArray()
        // last 29 bytes of addressData are hash of public key of this account
        addressData.takeLast(PUBLIC_KEY_HASH_LENGTH).toByteArray()
    }.toSet()
}

/**
 * Personas on network
 */
val GetProfileUseCase.personasOnCurrentNetwork
    get() = invoke().map { it.currentNetwork.personas }

suspend fun GetProfileUseCase.personasOnCurrentNetwork() = personasOnCurrentNetwork.first()

fun GetProfileUseCase.personaOnCurrentNetworkFlow(withAddress: String) =
    personasOnCurrentNetwork.mapNotNull { personas ->
        personas.firstOrNull { it.address == withAddress }
    }

suspend fun GetProfileUseCase.personaOnCurrentNetwork(
    withAddress: String
) = personasOnCurrentNetwork().firstOrNull { persona ->
    persona.address == withAddress
}

/**
 * Gateway preferences
 */
val GetProfileUseCase.gateways
    get() = invoke().map { it.appPreferences.gateways }

/**
 * Security preferences
 */
val GetProfileUseCase.security
    get() = invoke().map { it.appPreferences.security }

/**
 * P2P Links preferences
 */
val GetProfileUseCase.p2pLinks
    get() = invoke().map { it.appPreferences.p2pLinks }.distinctUntilChanged()
