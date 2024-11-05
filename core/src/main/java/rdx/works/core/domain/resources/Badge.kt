package rdx.works.core.domain.resources

import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample

data class Badge(
    val resource: Resource
) {

    val name: String?
        get() = resource.name.takeIf { it.isNotBlank() }

    companion object {

        @UsesSampleValues
        val sample: Sample<Badge>
            get() = object : Sample<Badge> {
                override fun invoke(): Badge = Badge(
                    resource = Resource.FungibleResource.sampleMainnet()
                )

                override fun other(): Badge = Badge(
                    resource = Resource.NonFungibleResource.sampleMainnet()
                )
            }
    }
}
