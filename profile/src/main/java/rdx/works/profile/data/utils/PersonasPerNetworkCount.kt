package rdx.works.profile.data.utils

import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.derivation.model.NetworkId

/**
 * Returns amount of personas on given networkId
 */
fun List<PerNetwork>.personasPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.personas?.count() ?: 0
