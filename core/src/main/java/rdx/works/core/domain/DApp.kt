package rdx.works.core.domain

import android.net.Uri
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.AccountType
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.accountType
import rdx.works.core.domain.resources.metadata.claimedEntities
import rdx.works.core.domain.resources.metadata.claimedWebsites
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name

data class DApp(
    val dAppAddress: AccountAddress,
    val lockerAddress: LockerAddress? = null,
    val metadata: List<Metadata> = listOf()
) {

    val name: String?
        get() = metadata.name()

    val description: String?
        get() = metadata.description()

    val iconUrl: Uri?
        get() = metadata.iconUrl()

    val isDappDefinition: Boolean
        get() = metadata.accountType() == AccountType.DAPP_DEFINITION

    val claimedWebsites: List<String>
        get() = metadata.claimedWebsites().orEmpty()

    val claimedEntities: List<String>
        get() = metadata.claimedEntities().orEmpty()

    @Suppress("SwallowedException")
    fun isRelatedWith(origin: String): Boolean {
        return claimedWebsites.any {
            try {
                val claimedUri = Uri.parse(it)
                val originUri = Uri.parse(origin)
                claimedUri.scheme != null && claimedUri.host == originUri.host
            } catch (e: Exception) {
                false
            }
        }
    }

    companion object {
        @UsesSampleValues
        val sampleMainnet: Sample<DApp> = object : Sample<DApp> {
            override fun invoke(): DApp = DApp(
                dAppAddress = AccountAddress.sampleMainnet(),
                metadata = listOf(
                    Metadata.Primitive(ExplicitMetadataKey.NAME.key, "Sample Mainnet DApp", MetadataType.String),
                    Metadata.Primitive(ExplicitMetadataKey.DESCRIPTION.key, "Description", MetadataType.String),
                    Metadata.Collection(
                        ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                        listOf(
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                                "https://hammunet-dashboard.rdx-works-main.extratools.works",
                                MetadataType.Origin
                            ),
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                                "https://ansharnet-dashboard.rdx-works-main.extratools.works",
                                MetadataType.Origin
                            ),
                        )
                    ),
                )
            )

            override fun other(): DApp = DApp(
                dAppAddress = AccountAddress.sampleMainnet.other(),
                metadata = listOf(
                    Metadata.Primitive(
                        key = ExplicitMetadataKey.NAME.key,
                        value = "Another dApp",
                        valueType = MetadataType.String
                    )
                )
            )
        }
    }
}
