package com.babylon.wallet.android.data.repository.cache

import com.babylon.wallet.android.data.repository.time.CurrentTime
import com.radixdlt.crypto.hash.sha256.extensions.sha256
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.KSerializer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import rdx.works.peerdroid.helpers.toHexString
import retrofit2.Call

internal class HttpCacheTest {

    private val memoryCacheClient = MemoryCacheClient()
    private val mockCurrentTime = mockk<CurrentTime>()
    private val testedClass = HttpCacheImpl(memoryCacheClient, mockCurrentTime)

    @Test
    fun `when a new call, the cache calculates the correct key and timestamp for the cached value`() {
        val value = FakeResponse()
        val now = Instant.now()
        val method = "GET"
        val url = "https://fake.com/fakeGet"
        every { mockCurrentTime.now() } returns now
        val mockApiCall = mockApiCall(method = method, url = url)
        val mockSerializer = mockk<KSerializer<FakeResponse>>()
        val key = arrayOf(method, url, "").contentToString().sha256().toHexString()

        testedClass.store(mockApiCall, value, mockSerializer)

        val restored = memoryCacheClient.read(key, mockSerializer)
        assertEquals(value, restored?.cached)
        assertEquals(now.toEpochMilli(), restored?.timestamp)
    }

    @Test
    fun `given a request happened 2 minutes ago, given a new request with the same details and cache tolerance of 5 minutes, the cached value is returned`() {
        val value = FakeResponse()
        val mockApiCall = mockApiCall()
        val mockSerializer = mockk<KSerializer<FakeResponse>>()

        val nowTime = Instant.now()
        val firstRequestTime = nowTime.minus(2, ChronoUnit.MINUTES)
        every { mockCurrentTime.now() } returns firstRequestTime
        testedClass.store(mockApiCall, value, mockSerializer)

        every { mockCurrentTime.now() } returns nowTime
        val cachedValue = testedClass.restore(mockApiCall, mockSerializer, timeoutDuration = Duration.of(5, ChronoUnit.MINUTES))

        assertEquals(value, cachedValue)
    }

    @Test
    fun `given a request happened 10 minutes ago, when a new request with the same details and cache tolerance of 5 minutes, then no value is returned because is stale`() {
        val value = FakeResponse()
        val mockApiCall = mockApiCall()
        val mockSerializer = mockk<KSerializer<FakeResponse>>()

        val nowTime = Instant.now()
        val firstRequestTime = nowTime.minus(10, ChronoUnit.MINUTES)
        every { mockCurrentTime.now() } returns firstRequestTime
        testedClass.store(mockApiCall, value, mockSerializer)

        every { mockCurrentTime.now() } returns nowTime
        val cachedValue = testedClass.restore(mockApiCall, mockSerializer, timeoutDuration = Duration.of(5, ChronoUnit.MINUTES))

        assertNull(cachedValue)
    }

    private fun mockApiCall(
        method: String = "POST",
        url: String = "https://fake.com/fake"
    ): Call<FakeResponse> {
        val apiCall = mockk<Call<FakeResponse>>()
        val mockRequest = mockk<Request>()
        every { apiCall.request() } returns mockRequest
        every { mockRequest.url } returns url.toHttpUrl()
        every { mockRequest.method } returns method
        every { mockRequest.body } returns null

        return apiCall
    }

    private data class FakeResponse(val name: String = "test")

    private class MemoryCacheClient: CacheClient {

        private val memory: MutableMap<String, Any> = mutableMapOf()

        override fun <T> write(key: String, value: CachedValue<T>, serializer: KSerializer<T>) {
            memory[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> read(key: String, serializer: KSerializer<T>): CachedValue<T>? {
            return memory[key] as? CachedValue<T>
        }

        override fun invalidate() {
            memory.clear()
        }

    }

}
