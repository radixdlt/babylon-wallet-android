package rdx.works.core.domain.assets

import android.net.Uri
import com.radixdlt.sargon.ValidatorAddress
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Validator

data class StakeClaim(
    val nonFungibleResource: Resource.NonFungibleResource,
    val validator: Validator
) : Asset.NonFungible {

    override val resource: Resource.NonFungibleResource
        get() = nonFungibleResource
    val validatorAddress: ValidatorAddress
        get() = validator.address

    val resourceAddress: String
        get() = nonFungibleResource.resourceAddress

    val name: String
        get() = nonFungibleResource.name

    val iconUrl: Uri?
        get() = nonFungibleResource.iconUrl

    fun unstakingNFTs(epoch: Long) = nonFungibleResource.items.filter { !it.isReadyToClaim(epoch) }
    fun readyToClaimNFTs(epoch: Long) = nonFungibleResource.items.filter { it.isReadyToClaim(epoch) }
}
