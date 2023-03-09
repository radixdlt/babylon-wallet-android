package com.babylon.wallet.android.data.repository.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

internal class CacheValueTestSerializerTest {

    @Test
    fun `given a serializable class, when the cached value is serialized, it is deserialized back correctly`() {
        val dataClass = DataClass(id = 0, data = "test")
        val time = 1677746443097
        val cachedValue = CachedValue(cached = dataClass, timestamp = time)

        val encoded = Json.encodeToString(
            CachedValueSerializer(DataClass.serializer()),
            cachedValue
        )
        val decodedCachedValue = Json.decodeFromString(
            CachedValueSerializer(DataClass.serializer()),
            encoded
        )

        assertEquals(dataClass, decodedCachedValue.cached)
        assertEquals(time, cachedValue.timestamp)
    }

    @Serializable
    private data class DataClass(
        val id: Int,
        val data: String,
    )
}
