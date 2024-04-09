package com.babylon.wallet.core.domain.assets

import com.radixdlt.derivation.model.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import junit.framework.TestCase.assertEquals
import org.junit.Test
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.assets.TokensPriceSorter
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

class TokensPriceSorterTest {

    private val xrdToken = Token(
        resource = Resource.FungibleResource(
            address = XrdResource.address(networkId = NetworkId.Mainnet.value),
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Radix", MetadataType.String),
                Metadata.Primitive("symbol", "xrd", MetadataType.String),
            )
        )
    )
    private val otherToken1 = Token(
        resource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.random(),
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Other 1", MetadataType.String),
                Metadata.Primitive("symbol", "OTR1", MetadataType.String),
            )
        )
    )
    private val otherToken2 = Token(
        resource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.random(),
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Other 2", MetadataType.String),
                Metadata.Primitive("symbol", "OTR2", MetadataType.String),
            )
        )
    )
    private val otherToken3 = Token(
        resource = Resource.FungibleResource(
            address = ResourceAddress.sampleMainnet.random(),
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Other 3", MetadataType.String),
                Metadata.Primitive("symbol", "OTR3", MetadataType.String),
            )
        )
    )
    private val allTokens = listOf(otherToken3, otherToken2, otherToken1, xrdToken)

    @Test
    fun `given no prices, the tokens are sorted based on their resources`() {
        val sut = TokensPriceSorter(pricesPerAsset = null)

        val result = allTokens.sortedWith(sut)

        assertEquals(
            listOf(xrdToken, otherToken1, otherToken2, otherToken3),
            result
        )
    }

    @Test
    fun `given some prices, the tokens are sorted based on prices first`() {
        val pricesPerAsset = mapOf<Asset, AssetPrice>(
            xrdToken to AssetPrice.TokenPrice(xrdToken, FiatPrice(1.toDecimal192(), SupportedCurrency.USD)),
            otherToken1 to AssetPrice.TokenPrice(otherToken1, null),
            otherToken2 to AssetPrice.TokenPrice(otherToken2, FiatPrice(20.toDecimal192(), SupportedCurrency.USD)),
            otherToken3 to AssetPrice.TokenPrice(otherToken3, FiatPrice(10.toDecimal192(), SupportedCurrency.USD)),
        )
        val sut = TokensPriceSorter(pricesPerAsset = pricesPerAsset)

        val result = allTokens.sortedWith(sut)

        // xrd is always first
        assertEquals(
            listOf(xrdToken, otherToken2, otherToken3, otherToken1),
            result
        )
    }

}