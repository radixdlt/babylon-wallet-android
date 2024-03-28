package rdx.works.core.domain.resources

import android.net.Uri
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name
import java.math.BigDecimal

data class Validator(
    val address: String,
    val totalXrdStake: BigDecimal?,
    val stakeUnitResourceAddress: String? = null,
    val claimTokenResourceAddress: String? = null,
    val metadata: List<Metadata> = emptyList()
) {
    val name: String
        get() = metadata.name().orEmpty()

    val url: Uri?
        get() = metadata.iconUrl()

    val description: String?
        get() = metadata.description()
}