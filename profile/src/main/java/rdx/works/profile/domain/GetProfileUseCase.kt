@file:Suppress("TooManyFunctions")

package rdx.works.profile.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.usesCurve25519
import rdx.works.profile.data.model.extensions.usesSecp256k1
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.EntityFlag
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceFlag
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.Entity
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.ret.AddressHelper
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileRepository: ProfileRepository) {
    operator fun invoke() = profileRepository.profile

    suspend fun isInitialized(): Boolean {
        return when (val profileState = profileRepository.profileState.first()) {
            ProfileState.NotInitialised,
            ProfileState.None -> false

            ProfileState.Incompatible -> true
            is ProfileState.Restored -> profileState.hasMainnet()
        }
    }
}

/**
 * Accounts on network
 */
val GetProfileUseCase.entitiesOnCurrentNetwork: Flow<List<Entity>>
    get() = invoke().map {
        it.currentNetwork?.accounts?.notHiddenAccounts().orEmpty() +
            it.currentNetwork?.personas?.notHiddenPersonas().orEmpty()
    }

suspend fun GetProfileUseCase.currentNetwork(): Network? {
    return invoke().firstOrNull()?.currentNetwork
}

val GetProfileUseCase.activeAccountsOnCurrentNetwork
    get() = invoke().map { it.currentNetwork?.accounts?.notHiddenAccounts().orEmpty() }

val GetProfileUseCase.hiddenAccountsOnCurrentNetwork
    get() = invoke().map {
        it.currentNetwork?.accounts?.filter { account -> account.flags.contains(EntityFlag.DeletedByUser) }.orEmpty()
    }

val GetProfileUseCase.factorSources
    get() = invoke().map { profile -> profile.factorSources }

val GetProfileUseCase.deviceFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<DeviceFactorSource>() }

private val GetProfileUseCase.deviceFactorSourcesWithAccounts
    get() = invoke().map { profile ->
        val deviceFactorSources = profile.factorSources.filterIsInstance<DeviceFactorSource>()
        val allAccountsOnNetwork = profile.currentNetwork?.accounts?.notHiddenAccounts().orEmpty()
        deviceFactorSources.associateWith { deviceFactorSource ->
            allAccountsOnNetwork.filter { it.factorSourceId == deviceFactorSource.id }
        }
    }

suspend fun GetProfileUseCase.mainBabylonFactorSource(): DeviceFactorSource? {
    val babylonFactorSources = deviceFactorSources.firstOrNull()?.filter {
        it.isBabylonDeviceFactorSource
    } ?: return null
    return if (babylonFactorSources.size == 1) {
        babylonFactorSources.first()
    } else {
        babylonFactorSources.firstOrNull { it.common.flags.contains(FactorSourceFlag.Main) }
    }
}

val GetProfileUseCase.olympiaFactorSourcesWithAccounts
    get() = deviceFactorSourcesWithAccounts.map {
        it.filter { entry ->
            entry.key.supportsOlympia
        }.mapValues { entry ->
            entry.value.filter { account -> account.usesSecp256k1 }
        }
    }

val GetProfileUseCase.babylonFactorSourcesWithAccounts
    get() = deviceFactorSourcesWithAccounts.map {
        it.filter { entry ->
            entry.key.isBabylonDeviceFactorSource
        }.mapValues { entry ->
            entry.value.filter { account -> account.usesCurve25519 }
        }
    }

val GetProfileUseCase.ledgerFactorSources
    get() = invoke().map { profile -> profile.factorSources.filterIsInstance<LedgerHardwareWalletFactorSource>() }

suspend fun GetProfileUseCase.factorSourceById(
    id: FactorSource.FactorSourceID
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.id == id
}

suspend fun GetProfileUseCase.deviceFactorSourceById(
    id: FactorSource.FactorSourceID
) = factorSources.first().filterIsInstance<DeviceFactorSource>().firstOrNull { factorSource ->
    factorSource.id == id
}

suspend fun GetProfileUseCase.factorSourceByIdValue(
    value: String
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.identifier == value
}

suspend fun GetProfileUseCase.accountsOnCurrentNetwork() = activeAccountsOnCurrentNetwork.first()

suspend fun GetProfileUseCase.accountOnCurrentNetwork(
    withAddress: String
) = accountsOnCurrentNetwork().firstOrNull { account ->
    account.address == withAddress
}

suspend fun GetProfileUseCase.currentNetworkAccountHashes(): Set<ByteArray> {
    return accountsOnCurrentNetwork().mapNotNull {
        AddressHelper.publicKeyHash(it.address)
    }.toSet()
}

/**
 * Personas on network
 */
val GetProfileUseCase.personasOnCurrentNetwork
    get() = invoke().map { it.currentNetwork?.personas?.notHiddenPersonas().orEmpty() }

val GetProfileUseCase.hiddenPersonasOnCurrentNetwork
    get() = invoke().map {
        it.currentNetwork?.personas?.filter { persona -> persona.flags.contains(EntityFlag.DeletedByUser) }.orEmpty()
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

val GetProfileUseCase.isBalanceVisible
    get() = invoke().map { it.appPreferences.display.isCurrencyAmountVisible }

/**
 * Default deposit guarantee
 */
suspend fun GetProfileUseCase.defaultDepositGuarantee() =
    invoke().map { it.appPreferences.transaction.defaultDepositGuarantee }.first()

fun Collection<Network.Account>.notHiddenAccounts(): List<Network.Account> {
    return filter { it.flags.contains(EntityFlag.DeletedByUser).not() }
}

fun Collection<Network.Persona>.notHiddenPersonas(): List<Network.Persona> {
    return filter { it.flags.contains(EntityFlag.DeletedByUser).not() }
}
