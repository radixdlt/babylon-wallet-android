package com.babylon.wallet.android.data.repository.cache

import com.radixdlt.crypto.hash.sha256.extensions.sha256
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okio.Buffer
import okio.IOException
import rdx.works.peerdroid.helpers.toHexString
import retrofit2.Call
import timber.log.Timber

data class CacheParameters(
    val httpCache: HttpCache,
    val override: Boolean = false,
    val timeoutDuration: Duration? = null
)

interface HttpCache {

    fun <T> store(call: Call<T>, response: T, serializationStrategy: KSerializer<T>)

    fun <T> restore(call: Call<T>, deserializationStrategy: KSerializer<T>, timeoutDuration: Duration?): T?
}

class HttpCacheImpl @Inject constructor(
    private val jsonSerializer: Json,
    private val cacheClient: CacheClient
) : HttpCache {

    private val logger = Timber.tag(TAG)

    override fun <T> store(
        call: Call<T>,
        response: T,
        serializationStrategy: KSerializer<T>
    ) {
        val key = call.cacheKey()
        val cachedValue = CachedValue(
            cached = response,
            timestamp = Date().time
        )
        val serialized = jsonSerializer.encodeToString(
            CachedValueSerializer(serializationStrategy),
            cachedValue
        )

        cacheClient.write(key, serialized)
    }

    override fun <T> restore(
        call: Call<T>,
        deserializationStrategy: KSerializer<T>,
        timeoutDuration: Duration?
    ): T? {
        val key = call.cacheKey()
        val restored = cacheClient.read(key)

        val cachedValue = restored?.let { saved ->
            try {
                jsonSerializer.decodeFromString(
                    CachedValueSerializer(deserializationStrategy),
                    saved
                )
            } catch (exception: IllegalArgumentException) {
                logger
                    .w("The value extracted belongs to a previous schema, cache value is considered stale")
                null
            }
        } ?: return null

        logger.d("--> [CACHE] ${call.request().method} - ${call.request().url}]")
        return if (timeoutDuration != null) {
            val threshold = LocalDateTime.now().minus(timeoutDuration)
            val minAllowedTime = Date.from(threshold.atZone(ZoneId.systemDefault()).toInstant()).time

            if (cachedValue.timestamp < minAllowedTime) {
                logger.d("<-- [CACHE] stale content")
                null
            } else {
                logger.d("<-- [CACHE] $restored")
                cachedValue.cached
            }
        } else {
            logger.d("<-- [CACHE] $restored")
            cachedValue.cached
        }
    }

    private fun Call<*>.cacheKey(): String {
        val method = request().method
        val url = request().url.toString()
        val body = request().body?.readUtf8().orEmpty()

        return arrayOf(method, url, body).contentToString().sha256().toHexString()
    }

    @Suppress("SwallowedException")
    private fun RequestBody.readUtf8(): String = try {
        val buffer = Buffer()

        this.writeTo(buffer)
        buffer.readUtf8()
    } catch (exception: IOException) {
        ""
    }

    companion object {
        private const val TAG = "HTTP_CACHE"
    }
}
