package rdx.works.profile.domain

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.DecodeAddressRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.DerivationPath
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import rdx.works.profile.data.utils.accountFactorSourceId
import rdx.works.profile.data.utils.getNextDerivationPathForAccount
import rdx.works.profile.data.utils.personaFactorSourceId
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
    get() = invoke().map { profile -> profile.factorSources.filter { it.kind == FactorSourceKind.DEVICE } }

val GetProfileUseCase.ledgerFactorSources
    get() = invoke().map { profile -> profile.factorSources.filter { it.kind == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET } }

suspend fun GetProfileUseCase.factorSource(
    id: FactorSource.ID
) = factorSources.first().firstOrNull { factorSource ->
    factorSource.id == id
}

suspend fun GetProfileUseCase.accountsOnCurrentNetwork() = accountsOnCurrentNetwork.first()

suspend fun GetProfileUseCase.accountOnCurrentNetwork(
    withAddress: String
) = accountsOnCurrentNetwork().firstOrNull { account ->
    account.address == withAddress
}

suspend fun GetProfileUseCase.nextDerivationPathForAccountOnCurrentNetwork(
    factorSource: FactorSource,
): DerivationPath {
    val currentNetwork = requireNotNull(invoke().first().currentNetwork.knownNetworkId)
    return factorSource.getNextDerivationPathForAccount(currentNetwork)
}

suspend fun GetProfileUseCase.accountFactorSourceIDOfDeviceKind(
    accountAddress: String,
): FactorSource.ID? {
    val accountFactorSourceID = accountOnCurrentNetwork(accountAddress)?.accountFactorSourceId()
    return deviceFactorSources.first().firstOrNull { it.id == accountFactorSourceID && it.kind == FactorSourceKind.DEVICE }?.id
}

suspend fun GetProfileUseCase.personaFactorSourceIDOfDeviceKind(
    personaAddress: String,
): FactorSource.ID? {
    val accountFactorSourceID = personaOnCurrentNetwork(personaAddress)?.personaFactorSourceId()
    return deviceFactorSources.first().firstOrNull { it.id == accountFactorSourceID && it.kind == FactorSourceKind.DEVICE }?.id
}

@Suppress("MagicNumber")
suspend fun GetProfileUseCase.currentNetworkAccountHashes(): Set<ByteArray> {
    return accountsOnCurrentNetwork().map {
        val addressData = RadixEngineToolkit.decodeAddress(DecodeAddressRequest(it.address)).getOrThrow().data
        // TODO change to addressData.drop(1) after RET update, to be consistent with iOS
        addressData.takeLast(26).toByteArray()
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
