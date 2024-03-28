package rdx.works.core.domain

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.AccountType
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.domain.resources.metadata.accountType
import rdx.works.core.domain.resources.metadata.claimedEntities
import rdx.works.core.domain.resources.metadata.claimedWebsites
import rdx.works.core.domain.resources.metadata.dAppDefinition
import rdx.works.core.domain.resources.metadata.description
import rdx.works.core.domain.resources.metadata.iconUrl
import rdx.works.core.domain.resources.metadata.name

data class DApp(
    val dAppAddress: AccountAddress,
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

    val definitionAddress: String?
        get() = metadata.dAppDefinition()

    val claimedWebsites: List<String>
        get() = metadata.claimedWebsites().orEmpty()

    val claimedEntities: List<String>
        get() = metadata.claimedEntities().orEmpty()

    val componentAddresses: List<String>
        get() = claimedEntities.filter { it.startsWith("component_") }

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
        @VisibleForTesting
        val sample: Sample<DApp> = object : Sample<DApp> {
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
                                MetadataType.Url
                            ),
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIMED_WEBSITES.key,
                                "https://ansharnet-dashboard.rdx-works-main.extratools.works",
                                MetadataType.Url
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
