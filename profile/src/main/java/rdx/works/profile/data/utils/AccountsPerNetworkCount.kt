package rdx.works.profile.data.utils

import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.derivation.model.NetworkId

/**
 * Returns amount of accounts on given networkId
 */
fun List<OnNetwork>.accountsPerNetworkCount(networkID: NetworkId): Int =
    find { it.networkID == networkID.value }?.accounts?.count() ?: 0
