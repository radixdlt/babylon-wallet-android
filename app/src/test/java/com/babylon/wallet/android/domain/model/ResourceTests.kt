package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.Resource.NonFungibleResource.Item
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test
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
        val unsortedList = listOf(
            fungibleResource(address = "1", name = "NoSymbolToken", symbol = null),
            fungibleResource(address = "9", name = null, symbol = "NNT"),
            fungibleResource(address = "8", name = null, symbol = null),
            fungibleResource(address = "4", name = "BigToken", symbol = null),
            fungibleResource(address = "5", name = "RadToken", symbol = null),
            fungibleResource(address = "7", name = "Another", symbol = "ANT"),
            fungibleResource(address = "6", name = "Another", symbol = "ANT"),
            fungibleResource(address = "2", name = null, symbol = "NNT"),
            fungibleResource(address = "3", name = null, symbol = null),
        )

        val sortedList = listOf(
            fungibleResource(address = "6", name = "Another", symbol = "ANT"),
            fungibleResource(address = "7", name = "Another", symbol = "ANT"),
            fungibleResource(address = "2", name = null, symbol = "NNT"),
            fungibleResource(address = "9", name = null, symbol = "NNT"),
            fungibleResource(address = "4", name = "BigToken", symbol = null),
            fungibleResource(address = "1", name = "NoSymbolToken", symbol = null),
            fungibleResource(address = "5", name = "RadToken", symbol = null),
            fungibleResource(address = "3", name = null, symbol = null),
            fungibleResource(address = "8", name = null, symbol = null),
        )

        Assert.assertEquals(sortedList, unsortedList.sorted())
    }

    @Test
    fun `given a list of non fungible collections, when displayed in a list, they are sorted correctly`() {
        val unsortedList = listOf(
            nonFungibleCollection(address = "2", name = "Big Collection", items = listOf(
                nft(collectionAddress = "2", "#12#"),
                nft(collectionAddress = "2", "#3#"),
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#2#")
            )),
            nonFungibleCollection(address = "3", name = null, items = listOf(
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "1", name = "Small Collection", items = listOf(
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "2", name = null, items = listOf(
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "4", name = null, items = listOf(
                nft(collectionAddress = "2", "<C>"),
                nft(collectionAddress = "2", "<A>"),
                nft(collectionAddress = "2", "<D>"),
                nft(collectionAddress = "2", "<B>")
            ))
        )

        val sortedList = listOf(
            nonFungibleCollection(address = "2", name = "Big Collection", items = listOf(
                nft(collectionAddress = "2", "#2#"),
                nft(collectionAddress = "2", "#3#"),
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#12#"),
            )),
            nonFungibleCollection(address = "1", name = "Small Collection", items = listOf(
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "2", name = null, items = listOf(
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "3", name = null, items = listOf(
                nft(collectionAddress = "2", "#1#"),
                nft(collectionAddress = "2", "#8#"),
                nft(collectionAddress = "2", "#10#"),
                nft(collectionAddress = "2", "#20#")
            )),
            nonFungibleCollection(address = "4", name = null, items = listOf(
                nft(collectionAddress = "2", "<A>"),
                nft(collectionAddress = "2", "<B>"),
                nft(collectionAddress = "2", "<C>"),
                nft(collectionAddress = "2", "<D>")
            ))
        )

        Assert.assertEquals(sortedList, unsortedList.sorted().map { it.copy(items = it.items.sorted()) })
    }

    @Test
    fun `given various nft ids, when a local id is created, then the correct type is assigned`() {
        Assert.assertEquals(Item.ID.IntegerType(1.toULong()), Item.ID.from("#1#"))
        Assert.assertEquals(Item.ID.StringType("Michael"), Item.ID.from("<Michael>"))
        Assert.assertEquals(Item.ID.BytesType("deadbeef"), Item.ID.from("[deadbeef]"))
        Assert.assertEquals(
            Item.ID.RUIDType("7b003d8e0b2c9e3a-516cf99882de64a1-f1cd6742ce3299e0-357f54f0333d25d0"),
            Item.ID.from("{7b003d8e0b2c9e3a-516cf99882de64a1-f1cd6742ce3299e0-357f54f0333d25d0}")
        )
    }

    @Test
    fun `given various nft ids, when a local id is initialised, then no exception is raised`() {
        Item.ID.from("#1#")
        Item.ID.from("<Michael>")
        Item.ID.from("[deadbeef]")
        Item.ID.from("{7b003d8e0b2c9e3a-516cf99882de64a1-f1cd6742ce3299e0-357f54f0333d25d0}")
    }

    @Test
    fun `verify that xrd resource shows only supply flexible`() {
        val resource = fungibleResource(
            address = XrdResource.address(),
            name = "Radix fungible token",
            symbol = "XRD"
        )

        assertEquals(setOf(AssetBehaviour.SUPPLY_FLEXIBLE), resource.behaviours)
    }

    @Test
    fun `verify that all resources except xrd show behaviours `() {
        val resource = fungibleResource(
            address = "resource_rdx_abcd",
            name = "Some fungible",
            symbol = "BUM"
        )

        Assert.assertEquals(resource.behaviours, setOf(
            AssetBehaviour.SUPPLY_FLEXIBLE,
            AssetBehaviour.INFORMATION_CHANGEABLE
        ))
    }

    private fun fungibleResource(
        address: String = "resource_rdx_abcd",
        name: String?,
        symbol: String?
    ) = Resource.FungibleResource(
        resourceAddress = address,
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
        address: String,
        name: String?,
        items: List<Item>
    ): Resource.NonFungibleResource = Resource.NonFungibleResource(
        resourceAddress = address,
        amount = items.size.toLong(),
        items = items,
        metadata = listOf(
            name?.let { Metadata.Primitive(ExplicitMetadataKey.NAME.key, it, MetadataType.String) }
        ).mapNotNull { it }
    )

    private fun nft(
        collectionAddress: String,
        localId: String
    ) = Item(
        collectionAddress = collectionAddress,
        localId = Item.ID.from(localId)
    )
}