package rdx.works.core.domain.resources

import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.metadata.Metadata

data class Pool(
    val address: String,
    val metadata: List<Metadata>,
    val resources: List<Resource.FungibleResource>,
    val associatedDApp: DApp? = null
)
