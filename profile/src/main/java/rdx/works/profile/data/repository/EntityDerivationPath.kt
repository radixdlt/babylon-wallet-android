package rdx.works.profile.data.repository

import rdx.works.profile.derivation.AccountHDDerivationPath
import rdx.works.profile.derivation.IdentityHDDerivationPath
import rdx.works.profile.derivation.model.KeyType
import rdx.works.profile.derivation.model.NetworkId

interface EntityDerivationPath {
    fun path(): String
}

class AccountDerivationPath(
    private val entityIndex: Int,
    private val networkId: NetworkId
) : EntityDerivationPath {

    override fun path(): String {
        return AccountHDDerivationPath(
            networkId = networkId,
            accountIndex = entityIndex,
            keyType = KeyType.TRANSACTION_SIGNING
        ).path
    }
}

class IdentityDerivationPath(
    private val entityIndex: Int,
    private val networkId: NetworkId
) : EntityDerivationPath {

    override fun path(): String {
        return IdentityHDDerivationPath(
            networkId = networkId,
            identityIndex = entityIndex,
            keyType = KeyType.TRANSACTION_SIGNING
        ).path
    }
}
