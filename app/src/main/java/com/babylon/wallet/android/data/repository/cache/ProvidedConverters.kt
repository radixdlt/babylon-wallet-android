package com.babylon.wallet.android.data.repository.cache

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.babylon.wallet.android.domain.model.behaviours.AssetBehaviour
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class TagsColumn(val tags: List<String>)

@ProvidedTypeConverter
class TagsColumnConverter {

    @TypeConverter
    fun stringToTags(string: String?): TagsColumn? {
        return string?.let { TagsColumn(tags = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun tagsToString(column: TagsColumn?): String? {
        return column?.let { Json.encodeToString(it.tags) }
    }

}

@Serializable
data class DappDefinitionsColumn(val dappDefinitions: List<String>)

@ProvidedTypeConverter
class DappDefinitionsColumnConverter {

    @TypeConverter
    fun stringToDappDefinitions(string: String?): DappDefinitionsColumn? {
        return string?.let { DappDefinitionsColumn(dappDefinitions = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun dAppDefinitionsToString(column: DappDefinitionsColumn?): String? {
        return column?.let { Json.encodeToString(it.dappDefinitions) }
    }

}

@Serializable
data class BehavioursColumn(val behaviours: List<AssetBehaviour>)

@ProvidedTypeConverter
class BehavioursColumnConverter {

    @TypeConverter
    fun stringToBehaviours(string: String?): BehavioursColumn? {
        return string?.let { BehavioursColumn(behaviours = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun behavioursToString(column: BehavioursColumn?): String? {
        return column?.let { Json.encodeToString(it.behaviours) }
    }

}

@Serializable
data class StringMetadataColumn(val metadata: List<Pair<String, String>>)

@ProvidedTypeConverter
class StringMetadataColumnConverter {

    @TypeConverter
    fun stringToStringMetadata(string: String?): StringMetadataColumn? {
        return string?.let { StringMetadataColumn(metadata = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun stringMetadataToString(column: StringMetadataColumn?): String? {
        return column?.let { Json.encodeToString(it.metadata) }
    }

}

@Serializable
data class NFTIdsColumn(val ids: List<String>)

@ProvidedTypeConverter
class NFTIdsColumnConverter {

    @TypeConverter
    fun stringToNFTIds(string: String?): NFTIdsColumn? {
        return string?.let { NFTIdsColumn(ids = Json.decodeFromString(string)) }
    }

    @TypeConverter
    fun nftIdsToString(column: NFTIdsColumn?): String? {
        return column?.let { Json.encodeToString(it.ids) }
    }

}

@ProvidedTypeConverter
class BigDecimalColumnConverter {

    @TypeConverter
    fun stringToBigDecimal(string: String?): BigDecimal? {
        return string?.toBigDecimalOrNull()
    }

    @TypeConverter
    fun bigDecimalToString(decimal: BigDecimal?): String? {
        return decimal?.toPlainString()
    }

}


@ProvidedTypeConverter
class InstantColumnConverter {

    @TypeConverter
    fun stringToInstant(dateString: String?): Instant? {
        return dateString?.let { Instant.parse(it) }
    }

    @TypeConverter
    fun instantToString(date: Instant?): String? {
        return date?.toString()
    }

}
