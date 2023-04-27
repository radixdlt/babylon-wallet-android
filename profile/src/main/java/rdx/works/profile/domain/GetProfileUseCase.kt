package rdx.works.profile.domain

import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.DecodeAddressRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
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
    get() = invoke().map { it.factorSources }

suspend fun GetProfileUseCase.accountsOnCurrentNetwork() = accountsOnCurrentNetwork.first()

suspend fun GetProfileUseCase.accountOnCurrentNetwork(
    withAddress: String
) = accountsOnCurrentNetwork().firstOrNull { account ->
    account.address == withAddress
}

@Suppress("MagicNumber")
suspend fun GetProfileUseCase.currentNetworkAccountHashes(): Set<ByteArray> {
    return accountsOnCurrentNetwork().map {
        val addressData = RadixEngineToolkit.decodeAddress(DecodeAddressRequest(it.address)).getOrThrow().data
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
    get() = invoke().map { it.appPreferences.p2pLinks }
