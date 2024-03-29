package rdx.works.core.domain.resources

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ValidatorAddress
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name
import java.math.BigDecimal

data class Validator(
    val address: ValidatorAddress,
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

    companion object {
        @VisibleForTesting
        val sampleMainnet: Sample<Validator> = object : Sample<Validator> {
            override fun invoke(): Validator = Validator(
                address = ValidatorAddress.sampleMainnet(),
                totalXrdStake = 10000.toBigDecimal(),
                stakeUnitResourceAddress = ResourceAddress.sampleMainnet.candy.string,
                claimTokenResourceAddress = ResourceAddress.sampleMainnet.nonFungibleGCMembership.string,
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        valueType = MetadataType.String,
                        value = "Sample Validator 1"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.ICON_URL.key,
                        valueType = MetadataType.Url,
                        value = "https://astrolescent.com/assets/img/babylon/astrolescent-badge.png"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.DESCRIPTION.key,
                        valueType = MetadataType.String,
                        value = "Validator 1 for tests and previews"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.CLAIM_NFT.key,
                        valueType = MetadataType.String,
                        value = ResourceAddress.sampleMainnet.nonFungibleGCMembership.string
                    )
                )
            )

            override fun other(): Validator = Validator(
                address = ValidatorAddress.sampleMainnet(),
                totalXrdStake = 20000.toBigDecimal(),
                stakeUnitResourceAddress = ResourceAddress.sampleMainnet.candy.string,
                claimTokenResourceAddress = ResourceAddress.sampleMainnet.nonFungibleGCMembership.string,
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        valueType = MetadataType.String,
                        value = "Sample Validator 2"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.ICON_URL.key,
                        valueType = MetadataType.Url,
                        value = "https://i.imgur.com/qJaLd7C.png"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.DESCRIPTION.key,
                        valueType = MetadataType.String,
                        value = "Validator 2 for tests and previews"
                    ),
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.CLAIM_NFT.key,
                        valueType = MetadataType.String,
                        value = ResourceAddress.sampleMainnet.nonFungibleGCMembership.string
                    )
                )
            )
        }
    }
}
