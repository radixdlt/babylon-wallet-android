package rdx.works.core.domain.resources

import android.net.Uri
import com.radixdlt.sargon.NonFungibleResourceAddress
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name

data class Badge(
    val address: NonFungibleResourceAddress,
    val metadata: List<Metadata> = emptyList()
) {

    val name: String?
        get() = metadata.name()

    val icon: Uri?
        get() = metadata.iconUrl()
}
