package rdx.works.profile.domain

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.method.DecodeAddressInput
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.DeviceFactorSource
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.factorsources.LedgerHardwareWalletFactorSource
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.data.utils.getNextDerivationPathForAccount
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val profileRepository: ProfileRepository) {
    operator fun invoke() = profileRepository.profile
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
        val deviceFactorSources = profile.factorSources.filter { it.id.kind == FactorSourceKind.DEVICE }
        val deviceFactorSourcesIds = deviceFactorSources.map { it.id }.toSet()
        profile.currentNetwork.accounts.filter { deviceFactorSourcesIds.contains(it.factorSourceId()) }.groupBy { it.factorSourceId() }
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

suspend fun GetProfileUseCase.nextDerivationPathForAccountOnCurrentNetworkWithLedger(
    ledgerHardwareWalletFactorSource: LedgerHardwareWalletFactorSource,
): DerivationPath {
    val currentNetwork = requireNotNull(invoke().first().currentNetwork.knownNetworkId)
    return ledgerHardwareWalletFactorSource.getNextDerivationPathForAccount(currentNetwork)
}

@Suppress("MagicNumber")
suspend fun GetProfileUseCase.currentNetworkAccountHashes(): Set<ByteArray> {
    return accountsOnCurrentNetwork().map {
        val addressData = RadixEngineToolkit.decodeAddress(DecodeAddressInput(it.address)).getOrThrow().data
        addressData.takeLast(29).toByteArray()
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
