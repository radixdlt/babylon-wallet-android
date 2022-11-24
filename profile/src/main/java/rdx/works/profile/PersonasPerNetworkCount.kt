package rdx.works.profile

import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.model.pernetwork.PerNetwork

/**
 * Returns amount of personas on given networkId
 */
fun List<PerNetwork>.personasPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.personas?.count() ?: 0
