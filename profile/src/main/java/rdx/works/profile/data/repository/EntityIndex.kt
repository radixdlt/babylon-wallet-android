package rdx.works.profile.data.repository

import rdx.works.profile.data.utils.accountsPerNetworkCount
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.data.model.pernetwork.PerNetwork
import rdx.works.profile.data.utils.personasPerNetworkCount

interface EntityIndex {
    fun index(): Int
}

class AccountIndex(
    val perNetwork: List<PerNetwork>,
    val networkId: NetworkId
) : EntityIndex {

    override fun index(): Int = perNetwork.accountsPerNetworkCount(networkId)
}

class PersonaIndex(
    val perNetwork: List<PerNetwork>,
    val networkId: NetworkId
) : EntityIndex {

    override fun index(): Int = perNetwork.personasPerNetworkCount(networkId)
}
