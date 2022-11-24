package rdx.works.profile

import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.model.pernetwork.PerNetwork

/**
 * Returns amount of accounts on given networkId
 */
fun List<PerNetwork>.accountsPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.accounts?.count() ?: 0
