package rdx.works.profile.data

import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.data.model.pernetwork.PerNetwork

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
