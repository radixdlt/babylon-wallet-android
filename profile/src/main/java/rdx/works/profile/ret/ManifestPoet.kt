package rdx.works.profile.ret

import rdx.works.core.AddressHelper
import rdx.works.profile.data.model.pernetwork.FactorInstance

object ManifestPoet {

    fun buildRola(
        entityAddress: String,
        publicKeyHashes: List<FactorInstance.PublicKey>
    ) = BabylonManifestBuilder()
        .setOwnerKeys(entityAddress, publicKeyHashes)
        .buildSafely(AddressHelper.networkId(entityAddress))
}