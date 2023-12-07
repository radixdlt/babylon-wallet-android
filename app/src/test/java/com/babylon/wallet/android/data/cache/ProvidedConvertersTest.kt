package com.babylon.wallet.android.data.cache

import com.babylon.wallet.android.data.repository.cache.database.BehavioursColumn
import com.babylon.wallet.android.data.repository.cache.database.MetadataColumn
import com.babylon.wallet.android.data.repository.cache.database.StateDatabaseConverters
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProvidedConvertersTest {

    @Test
    fun `test behaviours column conversion`() {
        val behavioursColumn = BehavioursColumn(behaviours = AssetBehaviour.values().toSet())
        val converter = StateDatabaseConverters()

        val string = converter.behavioursToString(behavioursColumn)
        val result = converter.stringToBehaviours(string)

        assertEquals(behavioursColumn, result)
    }

    @Test
    fun `test metadata column conversion`() {
        val metadataColumn = MetadataColumn(
            metadata = listOf(
                Metadata.Primitive("name", "Name of resource", MetadataType.String),
                Metadata.Primitive(
                    "icon_url",
                    "https://assets-global.website-files.com/6053f7fca5bf627283b582c2/6266da249c0c7cd4101e952a_Radix-Icon-400x400.png",
                    MetadataType.Url
                ),
                Metadata.Primitive(
                    "some_int",
                    "-1234",
                    MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.LONG)
                ),
                Metadata.Primitive(
                    "some_bool",
                    "false",
                    MetadataType.Bool
                ),
                Metadata.Primitive(
                    "some_int",
                    "-1234",
                    MetadataType.Integer(signed = true, size = MetadataType.Integer.Size.LONG)
                ),
                Metadata.Primitive(
                    "some_decimal",
                    "-1234.8974539875",
                    MetadataType.Decimal
                ),
                Metadata.Primitive(
                    "some_address",
                    "account_tdx_2_12xn3lgz7xv4d0d4cx25nvfekyxx0fsawhmtht0dd550vcu5wwl0g70",
                    MetadataType.Address
                ),
                Metadata.Primitive(
                    "some_global_id",
                    "resource_tdx_2_1nfkmpspv00vvt50al7rztwd29szsy83x5khhcwv2545khzj5ve344t:<Rand_0001>",
                    MetadataType.NonFungibleGlobalId
                ),
                Metadata.Primitive(
                    "some_local_id",
                    "{7cb9211651a973ce-bedc4e10307645a3-a30fdad450e4aec7-42fb51c60e662dbc}",
                    MetadataType.NonFungibleLocalId
                ),
                Metadata.Collection(
                    key = "some_collection",
                    values = listOf(
                        Metadata.Primitive("name1", "Name 1", MetadataType.String),
                        Metadata.Primitive("name2", "Name 2", MetadataType.String),
                        Metadata.Primitive("name3", "Name 3", MetadataType.String),
                        Metadata.Primitive("name4", "Name 4", MetadataType.String),
                    )
                ),
                Metadata.Map(
                    key = "some_collection",
                    values = mapOf(
                        Metadata.Primitive(
                            "account_tdx_2_12xn3lgz7xv4d0d4cx25nvfekyxx0fsawhmtht0dd550vcu5wwl0g70",
                            "ID1",
                            MetadataType.Address
                        ) to Metadata.Collection(
                            key = "names of id1",
                            values = listOf(
                                Metadata.Primitive("name1", "Name 1", MetadataType.String),
                                Metadata.Primitive("name2", "Name 2", MetadataType.String),
                                Metadata.Primitive("name3", "Name 3", MetadataType.String),
                                Metadata.Primitive("name4", "Name 4", MetadataType.String),
                            )
                        ),
                        Metadata.Primitive(
                            "another_primitive",
                            "Another Name",
                            MetadataType.String
                        ) to Metadata.Primitive("value", "Value", MetadataType.String)
                    )
                )
            )
        )
        val converter = StateDatabaseConverters()

        val string = converter.metadataToString(metadataColumn)
        val result = converter.stringToMetadata(string)

        assertEquals(metadataColumn, result)
    }

    @Test
    fun `assert null objects`() {
        with(StateDatabaseConverters()) {
            assertNull(stringToBehaviours(behavioursToString(null)))
            assertNull(metadataToString(stringToMetadata(null)))
        }
    }
}
