package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class TagsColumn(val tags: List<String>)

@Serializable
data class DappDefinitionsColumn(val dappDefinitions: List<String>)

@Serializable
data class BehavioursColumn(val behaviours: Set<AssetBehaviour>)

@Serializable
data class StringMetadataColumn(val metadata: List<Pair<String, String?>>)

@Serializable
data class NFTIdsColumn(val ids: List<String>)

@Suppress("TooManyFunctions")
@ProvidedTypeConverter
class StateDatabaseConverters {

    // TAGS
    @TypeConverter
    fun stringToTags(string: String?): TagsColumn? {
        return string?.let { TagsColumn(tags = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun tagsToString(column: TagsColumn?): String? {
        return column?.let { Json.encodeToString(it.tags) }
    }

    // DApp Definitions
    @TypeConverter
    fun stringToDappDefinitions(string: String?): DappDefinitionsColumn? {
        return string?.let { DappDefinitionsColumn(dappDefinitions = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun dAppDefinitionsToString(column: DappDefinitionsColumn?): String? {
        return column?.let { Json.encodeToString(it.dappDefinitions) }
    }

    // Behaviours
    @TypeConverter
    fun stringToBehaviours(string: String?): BehavioursColumn? {
        return string?.let { BehavioursColumn(behaviours = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun behavioursToString(column: BehavioursColumn?): String? {
        return column?.let { Json.encodeToString(it.behaviours) }
    }

    // String Metadata
    @TypeConverter
    fun stringToStringMetadata(string: String?): StringMetadataColumn? {
        return string?.let { StringMetadataColumn(metadata = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun stringMetadataToString(column: StringMetadataColumn?): String? {
        return column?.let { Json.encodeToString(it.metadata) }
    }

    // NFT IDs
    @TypeConverter
    fun stringToNFTIds(string: String?): NFTIdsColumn? {
        return string?.let { NFTIdsColumn(ids = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun nftIdsToString(column: NFTIdsColumn?): String? {
        return column?.let { Json.encodeToString(it.ids) }
    }

    // BigDecimal
    @TypeConverter
    fun stringToBigDecimal(string: String?): BigDecimal? {
        return string?.toBigDecimalOrNull()
    }

    @TypeConverter
    fun bigDecimalToString(decimal: BigDecimal?): String? {
        return decimal?.toPlainString()
    }

    // Instant
    @TypeConverter
    fun longToInstant(date: Long?): Instant? {
        return date?.let { Instant.ofEpochMilli(date) }
    }

    @TypeConverter
    fun instantToLong(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
}
