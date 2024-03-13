package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.AssetPrice
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.assets.TokensPriceSorter
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import junit.framework.TestCase.assertEquals
import org.junit.Test
import rdx.works.profile.derivation.model.NetworkId

class TokensPriceSorterTest {

    private val xrdToken = Token(
        resource = Resource.FungibleResource(
            resourceAddress = XrdResource.address(networkId = NetworkId.Mainnet),
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Radix", MetadataType.String),
                Metadata.Primitive("symbol", "xrd", MetadataType.String),
            )
        )
    )
    private val otherToken1 = Token(
        resource = Resource.FungibleResource(
            resourceAddress = "rdx_other1",
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Other 1", MetadataType.String),
                Metadata.Primitive("symbol", "OTR1", MetadataType.String),
            )
        )
    )
    private val otherToken2 = Token(
        resource = Resource.FungibleResource(
            resourceAddress = "rdx_other2",
            ownedAmount = null,
            metadata = listOf(
                Metadata.Primitive("name", "Other 2", MetadataType.String),
                Metadata.Primitive("symbol", "OTR2", MetadataType.String),
            )
        )
    )
    private val otherToken3 = Token(
        resource = Resource.FungibleResource(
            resourceAddress = "rdx_other3",
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
            xrdToken to AssetPrice.TokenPrice(xrdToken, 1.toBigDecimal(), "USD"),
            otherToken1 to AssetPrice.TokenPrice(otherToken1, null, null),
            otherToken2 to AssetPrice.TokenPrice(otherToken2, 20.toBigDecimal(), "USD"),
            otherToken3 to AssetPrice.TokenPrice(otherToken3, 10.toBigDecimal(), "USD"),
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