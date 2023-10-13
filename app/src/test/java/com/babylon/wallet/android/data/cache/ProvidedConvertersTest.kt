package com.babylon.wallet.android.data.cache

import com.babylon.wallet.android.data.repository.cache.BehavioursColumn
import com.babylon.wallet.android.data.repository.cache.BehavioursColumnConverter
import com.babylon.wallet.android.data.repository.cache.DappDefinitionsColumn
import com.babylon.wallet.android.data.repository.cache.DappDefinitionsColumnConverter
import com.babylon.wallet.android.data.repository.cache.StringMetadataColumn
import com.babylon.wallet.android.data.repository.cache.StringMetadataColumnConverter
import com.babylon.wallet.android.data.repository.cache.TagsColumn
import com.babylon.wallet.android.data.repository.cache.TagsColumnConverter
import com.babylon.wallet.android.domain.model.behaviours.AssetBehaviour
import org.junit.Assert.assertEquals
import org.junit.Test

class ProvidedConvertersTest {

    @Test
    fun `test tags column conversion`() {
        val tagsColumn = TagsColumn(tags = listOf("foo", "bar", "baz"))
        val converter = TagsColumnConverter()

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
        val converter = DappDefinitionsColumnConverter()

        val string = converter.dAppDefinitionsToString(dappDefinitionsColumn)
        val result = converter.stringToDappDefinitions(string)

        assertEquals(dappDefinitionsColumn, result)
    }

    @Test
    fun `test behaviours column conversion`() {
        val behavioursColumn = BehavioursColumn(behaviours = AssetBehaviour.values().toList())
        val converter = BehavioursColumnConverter()

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
        val converter = StringMetadataColumnConverter()

        val string = converter.stringMetadataToString(stringMetadataColumn)
        val result = converter.stringToStringMetadata(string)

        assertEquals(stringMetadataColumn, result)
    }

}
