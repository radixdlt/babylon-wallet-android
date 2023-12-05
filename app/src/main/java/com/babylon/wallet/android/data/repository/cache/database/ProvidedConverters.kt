package com.babylon.wallet.android.data.repository.cache.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.babylon.wallet.android.domain.model.assets.AssetBehaviour
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class BehavioursColumn(val behaviours: Set<AssetBehaviour>)

@Serializable
data class MetadataColumn(val metadata: List<Metadata>)

@Suppress("TooManyFunctions")
@ProvidedTypeConverter
class StateDatabaseConverters {

    private val json = Json {
        allowStructuredMapKeys = true
    }

    // Behaviours
    @TypeConverter
    fun stringToBehaviours(string: String?): BehavioursColumn? {
        return string?.let { BehavioursColumn(behaviours = json.decodeFromString(string)) }
    }

    @TypeConverter
    fun behavioursToString(column: BehavioursColumn?): String? {
        return column?.let { json.encodeToString(it.behaviours) }
    }

    // Metadata
    @TypeConverter
    fun stringToMetadata(string: String?): MetadataColumn? {
        return string?.let { MetadataColumn(metadata = json.decodeFromString(string)) }
    }

    @TypeConverter
    fun metadataToString(column: MetadataColumn?): String? {
        return column?.let { json.encodeToString(it.metadata) }
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
