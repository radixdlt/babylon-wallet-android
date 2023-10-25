package com.babylon.wallet.android.data.cache

import com.babylon.wallet.android.data.repository.cache.database.BehavioursColumn
import com.babylon.wallet.android.data.repository.cache.database.DappDefinitionsColumn
import com.babylon.wallet.android.data.repository.cache.database.NFTIdsColumn
import com.babylon.wallet.android.data.repository.cache.database.StateDatabaseConverters
import com.babylon.wallet.android.data.repository.cache.database.StringMetadataColumn
import com.babylon.wallet.android.data.repository.cache.database.TagsColumn
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProvidedConvertersTest {

    @Test
    fun `test tags column conversion`() {
        val tagsColumn = TagsColumn(tags = listOf("foo", "bar", "baz"))
        val converter = StateDatabaseConverters()

        val string = converter.tagsToString(tagsColumn)
        val result = converter.stringToTags(string)

        assertEquals(tagsColumn, result)
    }

    @Test
    fun `test dapp definitions column conversion`() {
        val dappDefinitionsColumn = DappDefinitionsColumn(dappDefinitions =
            listOf(
                "account_tdx_2_128vqf2fes4y7zpj5yfndmuzhavlyn6wvld4vxurv7pg4psz4wv0laj",
                "account_tdx_2_128vqf2fes4y7zpj5yfndmuzhavlyn6abcd4vxurv7pg4psz4wv0laj",
                "account_tdx_2_128vqf2fes4y7zpj5yfndmuzhavlyn6acda4vxurv7pg4psz4wv0laj"
            )
        )
        val converter = StateDatabaseConverters()

        val string = converter.dAppDefinitionsToString(dappDefinitionsColumn)
        val result = converter.stringToDappDefinitions(string)

        assertEquals(dappDefinitionsColumn, result)
    }

    @Test
    fun `test behaviours column conversion`() {
        val behavioursColumn = BehavioursColumn(behaviours = AssetBehaviour.values().toSet())
        val converter = StateDatabaseConverters()

        val string = converter.behavioursToString(behavioursColumn)
        val result = converter.stringToBehaviours(string)

        assertEquals(behavioursColumn, result)
    }

    @Test
    fun `test string metadata column conversion`() {
        val stringMetadataColumn = StringMetadataColumn(metadata = listOf(
            "one" to "abcde",
            "two" to "10",
            "-%$#" to "%$#%$*&^*,"
        ))
        val converter = StateDatabaseConverters()

        val string = converter.stringMetadataToString(stringMetadataColumn)
        val result = converter.stringToStringMetadata(string)

        assertEquals(stringMetadataColumn, result)
    }

    @Test
    fun `test nft ids column conversion`() {
        val nftIdsColumn = NFTIdsColumn(ids = listOf(
            "#1#",
            "#2#",
            "#3#"
        ))
        val converter = StateDatabaseConverters()

        val string = converter.nftIdsToString(nftIdsColumn)
        val result = converter.stringToNFTIds(string)

        assertEquals(nftIdsColumn, result)
    }

    @Test
    fun `assert null objects`() {
        with(StateDatabaseConverters()) {
            assertNull(stringToTags(tagsToString(null)))
            assertNull(stringToDappDefinitions(dAppDefinitionsToString(null)))
            assertNull(stringToBehaviours(behavioursToString(null)))
            assertNull(stringToStringMetadata(stringMetadataToString(null)))
            assertNull(stringToNFTIds(nftIdsToString(null)))
        }
    }
}
