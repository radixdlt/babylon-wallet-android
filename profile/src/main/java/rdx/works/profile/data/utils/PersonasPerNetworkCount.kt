package rdx.works.profile.data.utils

import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.derivation.model.NetworkId

/**
 * Returns amount of personas on given networkId
 */
fun List<OnNetwork>.personasPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.personas?.count() ?: 0
