package rdx.works.profile.data.utils

import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.data.model.pernetwork.PerNetwork

/**
 * Returns amount of accounts on given networkId
 */
fun List<PerNetwork>.accountsPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.accounts?.count() ?: 0
