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

object TimeoutDuration {

    private const val ZERO_SECONDS_TIME = 0L
    private const val ONE_MINUTE_TIME = 1L
    private const val FIVE_MINUTES_TIME = 5L

    val NO_CACHE: Duration = Duration.ofSeconds(ZERO_SECONDS_TIME)

    val ONE_MINUTE: Duration = Duration.ofMinutes(ONE_MINUTE_TIME)

    val FIVE_MINUTES: Duration = Duration.ofMinutes(FIVE_MINUTES_TIME)
}

data class CacheParameters(
    /**
     * This is the instance of the cache, injected from the repository
     */
    val httpCache: HttpCache,
    /**
     * This is the maximum duration that the cache is considered active.
     * * If the time passed is more than this duration, then the content is considered stale.
     * * If [TimeoutDuration.NO_CACHE] is received, then the cache should be overridden,
     *   since there is no point in checking the cache.
     * * If [null] is received then the content has no timeout information, meaning that
     *   if it exists in the cache, it will always return that value
     */
    val timeoutDuration: Duration? = null
) {

    val isCacheOverridden: Boolean
        get() = timeoutDuration?.isZero == true
}

interface HttpCache {

    fun <T> store(call: Call<T>, response: T, serializer: KSerializer<T>)

    fun <T> restore(call: Call<T>, serializer: KSerializer<T>, timeoutDuration: Duration?): T?

    fun invalidate()
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

    override fun invalidate() {
        cacheClient.invalidate()
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
