package com.babylon.wallet.android.data.repository.cache

import kotlinx.serialization.KSerializer
import okhttp3.HttpUrl
import okhttp3.RequestBody
import okio.Buffer
import okio.IOException
import rdx.works.core.InstantGenerator
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import retrofit2.Call
import timber.log.Timber
import java.net.URL
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

    suspend fun <T> store(call: Call<T>, response: T, serializer: KSerializer<T>)

    suspend fun <T> restore(call: Call<T>, serializer: KSerializer<T>, timeoutDuration: Duration?): T?

    fun invalidate()
}

class HttpCacheImpl @Inject constructor(
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val cacheClient: CacheClient
) : HttpCache {

    private val logger = Timber.tag(TAG)

    override suspend fun <T> store(
        call: Call<T>,
        response: T,
        serializer: KSerializer<T>
    ) {
        val cacheKeyData = call.cacheKeyData()
        val now = InstantGenerator().toEpochMilli()

        val cachedValue = CachedValue(
            cached = response,
            timestamp = now
        )

        cacheClient.write(cacheKeyData.toKey(), cachedValue, serializer)
    }

    override suspend fun <T> restore(
        call: Call<T>,
        serializer: KSerializer<T>,
        timeoutDuration: Duration?
    ): T? {
        val cacheKeyData = call.cacheKeyData()
        logger.d("--> [CACHE] ${cacheKeyData.method} - ${cacheKeyData.url}")

        val cachedValue = cacheClient.read(cacheKeyData.toKey(), serializer) ?: run {
            logger.d("<-- [CACHE] ❌ no value")
            return null
        }

        return if (timeoutDuration != null) {
            val threshold = InstantGenerator().minus(timeoutDuration)
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

    private suspend fun Call<*>.cacheKeyData(): CacheKeyData {
        val baseUrl = URL(getCurrentGatewayUseCase().url)

        return CacheKeyData(
            method = request().method,
            url = request().url.newBuilder().host(baseUrl.host).scheme(baseUrl.protocol).build(),
            body = request().body
        )
    }

    private data class CacheKeyData(
        val method: String,
        val url: HttpUrl,
        val body: RequestBody?
    ) {

        fun toKey() = arrayOf(
            method,
            url.toString(),
            body?.readUtf8().orEmpty()
        ).contentToString().blake2Hash().toHexString()

        @Suppress("SwallowedException")
        private fun RequestBody.readUtf8(): String = try {
            val buffer = Buffer()

            this.writeTo(buffer)
            buffer.readUtf8()
        } catch (exception: IOException) {
            ""
        }
    }

    companion object {
        private const val TAG = "HTTP_CACHE"
    }
}
