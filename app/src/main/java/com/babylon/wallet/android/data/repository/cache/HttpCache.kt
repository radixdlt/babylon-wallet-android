package com.babylon.wallet.android.data.repository.cache

import com.babylon.wallet.android.data.repository.time.CurrentTime
import com.radixdlt.crypto.hash.sha256.extensions.sha256
import kotlinx.serialization.KSerializer
import okhttp3.RequestBody
import okio.Buffer
import okio.IOException
import rdx.works.peerdroid.helpers.toHexString
import retrofit2.Call
import timber.log.Timber
import java.time.Duration
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class CacheParameters(
    val httpCache: HttpCache,
    val override: Boolean = false,
    val timeoutDuration: Duration? = null
)

interface HttpCache {

    fun <T> store(call: Call<T>, response: T, serializer: KSerializer<T>)

    fun <T> restore(call: Call<T>, serializer: KSerializer<T>, timeoutDuration: Duration?): T?
}

class HttpCacheImpl @Inject constructor(
    private val cacheClient: CacheClient,
    private val currentTime: CurrentTime
) : HttpCache {

    private val logger = Timber.tag(TAG)

    override fun <T> store(
        call: Call<T>,
        response: T,
        serializer: KSerializer<T>
    ) {
        val key = call.cacheKey()
        val now = currentTime.now().toEpochMilli()

        val cachedValue = CachedValue(
            cached = response,
            timestamp = now
        )

        cacheClient.write(key, cachedValue, serializer)
    }

    override fun <T> restore(
        call: Call<T>,
        serializer: KSerializer<T>,
        timeoutDuration: Duration?
    ): T? {
        logger.d("--> [CACHE] ${call.request().method} - ${call.request().url}")

        val key = call.cacheKey()
        val cachedValue = cacheClient.read(key, serializer) ?: run {
            logger.d("<-- [CACHE] ❌ no value")
            return null
        }

        return if (timeoutDuration != null) {
            val threshold = currentTime.now().minus(timeoutDuration)
            val minAllowedTime = Date.from(threshold.atZone(ZoneId.systemDefault()).toInstant()).time

            if (cachedValue.timestamp < minAllowedTime) {
                logger.d("<-- [CACHE] ❌ stale content")
                null
            } else {
                logger.d("<-- [CACHE] ✅ active content")
                cachedValue.cached
            }
        } else {
            logger.d("<-- [CACHE] ✅ active content")
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
