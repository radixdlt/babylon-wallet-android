package com.babylon.wallet.core.domain.resources

import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.bytesId
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.intId
import com.radixdlt.sargon.extensions.ruidId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.stringId
import com.radixdlt.sargon.samples.sampleMainnet
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test
import rdx.works.core.domain.assets.AssetBehaviour
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Resource.NonFungibleResource.Item
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import java.math.BigDecimal

class ResourceTests {

    @Test
    fun `given a fungible resource, when not null symbol and not null name, then display title has the symbol value`() {
        val resource = fungibleResource(name = "name", symbol = "SYM")

        Assert.assertEquals(resource.symbol, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when not null symbol and null name, then display title has the symbol value`() {
        val resource = fungibleResource(name = null, symbol = "SYM")

        Assert.assertEquals(resource.symbol, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when null symbol and not null name, then display title has the name value`() {
        val resource = fungibleResource(name = "name", symbol = null)

        Assert.assertEquals(resource.name, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when null symbol and null name, then display title has empty value`() {
        val resource = fungibleResource(name = null, symbol = null)

        Assert.assertEquals("", resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when blank symbol and not null name or blank, then display title has name value`() {
        val resource = fungibleResource(name = "name", symbol = " ")

        Assert.assertEquals(resource.name, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when blank symbol and empty name, then display title has empty value`() {
        val resource = fungibleResource(name = "", symbol = " ")

        Assert.assertEquals("", resource.displayTitle)
    }

    @Test
    fun `given a list of fungible resources, when displayed in a list, they are sorted correctly`() {
        val resourceAddresses = List(9) {
            ResourceAddress.sampleMainnet.random()
        }.sortedBy { it.string }
        val unsortedList = listOf(
            fungibleResource(address = resourceAddresses[0], name = "NoSymbolToken", symbol = null),
            fungibleResource(address = resourceAddresses[8], name = null, symbol = "NNT"),
            fungibleResource(address = resourceAddresses[7], name = null, symbol = null),
            fungibleResource(address = resourceAddresses[3], name = "BigToken", symbol = null),
            fungibleResource(address = resourceAddresses[4], name = "RadToken", symbol = null),
            fungibleResource(address = resourceAddresses[6], name = "Another", symbol = "ANT"),
            fungibleResource(address = resourceAddresses[5], name = "Another", symbol = "ANT"),
            fungibleResource(address = resourceAddresses[1], name = null, symbol = "NNT"),
            fungibleResource(address = resourceAddresses[2], name = null, symbol = null),
        )

        val sortedList = listOf(
            fungibleResource(address = resourceAddresses[5], name = "Another", symbol = "ANT"),
            fungibleResource(address = resourceAddresses[6], name = "Another", symbol = "ANT"),
            fungibleResource(address = resourceAddresses[1], name = null, symbol = "NNT"),
            fungibleResource(address = resourceAddresses[8], name = null, symbol = "NNT"),
            fungibleResource(address = resourceAddresses[3], name = "BigToken", symbol = null),
            fungibleResource(address = resourceAddresses[0], name = "NoSymbolToken", symbol = null),
            fungibleResource(address = resourceAddresses[4], name = "RadToken", symbol = null),
            fungibleResource(address = resourceAddresses[2], name = null, symbol = null),
            fungibleResource(address = resourceAddresses[7], name = null, symbol = null),
        )

        Assert.assertEquals(sortedList, unsortedList.sorted())
    }

    @Test
    fun `given a list of non fungible collections, when displayed in a list, they are sorted correctly`() {
        val resourceAddress1 = ResourceAddress.sampleMainnet.random()
        val resourceAddress2 = ResourceAddress.sampleMainnet.random()
        val resourceAddress3 = ResourceAddress.sampleMainnet.random()
        val resourceAddress4 = ResourceAddress.sampleMainnet.random()
        val unsortedList = listOf(
            nonFungibleCollection(address = resourceAddress2, name = "Name 2", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#12#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#3#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#2#")))
            )),
            nonFungibleCollection(address = resourceAddress3, name = "Name 3", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#1#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#8#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#20#")))
            )),
            nonFungibleCollection(address = resourceAddress1, name = "Name 1", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#1#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#8#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#20#")))
            )),
            nonFungibleCollection(address = resourceAddress4, name = "Name 4", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<C>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<A>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<D>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<B>")))
            ))
        )

        val sortedList = listOf(
            nonFungibleCollection(address = resourceAddress1, name = "Name 1", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#1#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#8#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress1, NonFungibleLocalId.init("#20#")))
            )),
            nonFungibleCollection(address = resourceAddress2, name = "Name 2", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#2#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#3#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("#12#"))),
            )),
            nonFungibleCollection(address = resourceAddress3, name = "Name 3", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#1#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#8#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#10#"))),
                nft(NonFungibleGlobalId(resourceAddress3, NonFungibleLocalId.init("#20#")))
            )),
            nonFungibleCollection(address = resourceAddress4, name = "Name 4", items = listOf(
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<A>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<B>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<C>"))),
                nft(NonFungibleGlobalId(resourceAddress2, NonFungibleLocalId.init("<D>"))),
            ))
        )

        Assert.assertEquals(sortedList, unsortedList.sorted().map { it.copy(items = it.items.sorted()) })
    }

    @Test
    fun `given various nft ids, when a local id is created, then the correct type is assigned`() {
        Assert.assertEquals(NonFungibleLocalId.intId(1.toULong()), NonFungibleLocalId.init("#1#"))
        Assert.assertEquals(NonFungibleLocalId.stringId("Michael"), NonFungibleLocalId.init("<Michael>"))
        Assert.assertEquals(NonFungibleLocalId.bytesId("deadbeef".hexToBagOfBytes()), NonFungibleLocalId.init("[deadbeef]"))
        Assert.assertEquals(
            NonFungibleLocalId.ruidId("7b003d8e0b2c9e3a516cf99882de64a1f1cd6742ce3299e0357f54f0333d25d0".hexToBagOfBytes()),
            NonFungibleLocalId.init("{7b003d8e0b2c9e3a-516cf99882de64a1-f1cd6742ce3299e0-357f54f0333d25d0}")
        )
    }

    @Test
    fun `verify that xrd resource shows only supply flexible`() {
        val resource = fungibleResource(
            address = XrdResource.address(networkId = 1),
            name = "Radix fungible token",
            symbol = "XRD"
        )

        assertEquals(setOf(AssetBehaviour.SUPPLY_FLEXIBLE), resource.behaviours)
    }

    @Test
    fun `verify that all resources except xrd show behaviours `() {
        val resource = fungibleResource(
            address = ResourceAddress.sampleMainnet.random(),
            name = "Some fungible",
            symbol = "BUM"
        )

        Assert.assertEquals(resource.behaviours, setOf(
            AssetBehaviour.SUPPLY_FLEXIBLE,
            AssetBehaviour.INFORMATION_CHANGEABLE
        ))
    }

    private fun fungibleResource(
        address: ResourceAddress = ResourceAddress.sampleMainnet.random(),
        name: String?,
        symbol: String?
    ) = Resource.FungibleResource(
        address = address,
        ownedAmount = BigDecimal(1234.5678),
        assetBehaviours = setOf(
            AssetBehaviour.SUPPLY_FLEXIBLE,
            AssetBehaviour.INFORMATION_CHANGEABLE
        ),
        metadata = name?.let {
            listOf(Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = it, valueType = MetadataType.String))
        }.orEmpty() + symbol?.let {
            listOf(Metadata.Primitive(key = ExplicitMetadataKey.SYMBOL.key, value = it, valueType = MetadataType.String))
        }.orEmpty()

    )

    private fun nonFungibleCollection(
        address: ResourceAddress,
        name: String?,
        items: List<Item>
    ): Resource.NonFungibleResource = Resource.NonFungibleResource(
        address = address,
        amount = items.size.toLong(),
        items = items,
        metadata = listOf(
            name?.let { Metadata.Primitive(ExplicitMetadataKey.NAME.key, it, MetadataType.String) }
        ).mapNotNull { it }
    )

    private fun nft(
        globalId: NonFungibleGlobalId
    ) = Item(
        collectionAddress = globalId.resourceAddress,
        localId = globalId.nonFungibleLocalId
    )
}
