package com.babylon.wallet.android.domain.model

import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.model.TokenUiModel
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class ResourceTests {

    @Test
    fun `given a fungible resource, when not null symbol and not null name, then display title has the symbol value`() {
        val resource = fungibleResource("name", "SYM")

        Assert.assertEquals(resource.symbol, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when not null symbol and null name, then display title has the symbol value`() {
        val resource = fungibleResource(null, "SYM")

        Assert.assertEquals(resource.symbol, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when null symbol and not null name, then display title has the name value`() {
        val resource = fungibleResource("name", null)

        Assert.assertEquals(resource.name, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when null symbol and null name, then display title has empty value`() {
        val resource = fungibleResource(null, null)

        Assert.assertEquals("", resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when blank symbol and not null name or blank, then display title has name value`() {
        val resource = fungibleResource("name", " ")

        Assert.assertEquals(resource.name, resource.displayTitle)
    }

    @Test
    fun `given a fungible resource, when blank symbol and empty name, then display title has empty value`() {
        val resource = fungibleResource("", " ")

        Assert.assertEquals("", resource.displayTitle)
    }

    private fun fungibleResource(name: String?, symbol: String?) = Resource.FungibleResource(
        resourceAddress = "resource_rdx_abcd",
        amount = BigDecimal(1234.5678),
        nameMetadataItem = name?.let { NameMetadataItem(it) },
        symbolMetadataItem = symbol?.let { SymbolMetadataItem(it) }
    )
}
