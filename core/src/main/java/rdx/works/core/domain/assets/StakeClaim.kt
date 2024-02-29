package rdx.works.core.domain.assets

import android.net.Uri
import rdx.works.core.domain.resources.Resource

data class StakeClaim(
    val nonFungibleResource: Resource.NonFungibleResource,
    val validator: ValidatorDetail
) : Asset.NonFungible {

    override val resource: Resource.NonFungibleResource
        get() = nonFungibleResource
    val validatorAddress: String
        get() = nonFungibleResource.validatorAddress.orEmpty()

    val resourceAddress: String
        get() = nonFungibleResource.resourceAddress

    val name: String
        get() = nonFungibleResource.name

    val iconUrl: Uri?
        get() = nonFungibleResource.iconUrl

    fun unstakingNFTs(epoch: Long) = nonFungibleResource.items.filter { !it.isReadyToClaim(epoch) }
    fun readyToClaimNFTs(epoch: Long) = nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
}
