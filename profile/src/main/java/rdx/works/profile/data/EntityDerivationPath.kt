package rdx.works.profile.data

import rdx.works.profile.derivation.AccountHDDerivationPath
import rdx.works.profile.derivation.IdentityHDDerivationPath
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.data.model.pernetwork.PerNetwork

interface EntityDerivationPath {
    fun path(): String
}

class AccountDerivationPath(
    val perNetwork: List<PerNetwork>,
    val networkId: NetworkId
) : EntityDerivationPath {

    override fun path(): String {
        val entityIndex = perNetwork.accountsPerNetworkCount(networkId)
        return AccountHDDerivationPath(
            networkId = networkId,
            accountIndex = entityIndex,
            keyType = KeyType.SignTransaction
        ).path
    }
}

class IdentityDerivationPath(
    val perNetwork: List<PerNetwork>,
    val networkId: NetworkId
) : EntityDerivationPath {

    override fun path(): String {
        val entityIndex = perNetwork.personasPerNetworkCount(networkId)
        return IdentityHDDerivationPath(
            networkId = networkId,
            identityIndex = entityIndex,
            keyType = KeyType.SignTransaction
        ).path
    }
}
