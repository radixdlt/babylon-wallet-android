@file:Suppress("TooManyFunctions")

package rdx.works.profile.domain

import com.radixdlt.ret.Address
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.core.PUBLIC_KEY_HASH_LENGTH
import rdx.works.core.toByteArray
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.nextAccountIndex
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
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
    get() = invoke().map { it.currentNetwork.accounts.notHiddenAccounts() }

val GetProfileUseCase.hiddenAccountsOnCurrentNetwork
    get() = invoke().map {
        it.currentNetwork.accounts.filter { account -> account.flags.contains(EntityFlag.DeletedByUser) }
    }

val GetProfileUseCase.factorSources
    get() = invoke().map { profile -> profile.factorSources }

val GetProfileUseCase.deviceFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<DeviceFactorSource>() }

val GetProfileUseCase.deviceFactorSourcesWithAccounts
    get() = invoke().map { profile ->
        val deviceFactorSources = profile.factorSources.filterIsInstance<DeviceFactorSource>()
        val allAccountsOnNetwork = profile.currentNetwork.accounts.notHiddenAccounts()
        deviceFactorSources.associateWith { deviceFactorSource ->
            allAccountsOnNetwork.filter { it.factorSourceId() == deviceFactorSource.id }
        }
    }

val GetProfileUseCase.ledgerFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<LedgerHardwareWalletFactorSource>() }

suspend fun GetProfileUseCase.factorSourceById(
    id: FactorSource.FactorSourceID
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.id == id
}

suspend fun GetProfileUseCase.factorSourceByIdValue(
    value: String
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.identifier == value
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
    get() = invoke().map { it.currentNetwork.personas.notHiddenPersonas() }

val GetProfileUseCase.hiddenPersonasOnCurrentNetwork
    get() = invoke().map {
        it.currentNetwork.personas.filter { persona -> persona.flags.contains(EntityFlag.DeletedByUser) }
    }

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
    get() = invoke().map { it.appPreferences.p2pLinks }

/**
 * Default deposit guarantee
 */
suspend fun GetProfileUseCase.defaultDepositGuarantee() =
    invoke().map { it.appPreferences.transaction.defaultDepositGuarantee }.first()

private fun List<Network.Account>.notHiddenAccounts(): List<Network.Account> {
    return filter { it.flags.contains(EntityFlag.DeletedByUser).not() }
}

private fun List<Network.Persona>.notHiddenPersonas(): List<Network.Persona> {
    return filter { it.flags.contains(EntityFlag.DeletedByUser).not() }
}
