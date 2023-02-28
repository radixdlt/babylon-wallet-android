package com.babylon.wallet.android.data.repository.cache

import android.content.Context
import com.radixdlt.crypto.hash.sha256.extensions.sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.concurrent.TaskRunner
import okio.Buffer
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toOkioPath
import okio.buffer
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.peerdroid.helpers.toHexString
import retrofit2.Call
import timber.log.Timber
import javax.inject.Inject

interface HttpCache {

    fun <T> store(call: Call<T>, response: T, serializationStrategy: SerializationStrategy<T>)

    fun <T> restore(call: Call<T>, deserializationStrategy: DeserializationStrategy<T>): T?
}

class HttpCacheImpl @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val jsonSerializer: Json
) : HttpCache {

    private val diskCache: DiskLruCache = DiskLruCache(
        fileSystem = FileSystem.SYSTEM,
        directory = applicationContext.cacheDir.toOkioPath(),
        appVersion = CACHE_VERSION,
        valueCount = MAX_VALUES_PER_KEY,
        maxSize = DEFAULT_CACHE_MAX_SIZE,
        taskRunner = TaskRunner.INSTANCE
    )

    override fun <T> store(
        call: Call<T>,
        response: T,
        serializationStrategy: SerializationStrategy<T>
    ) {
        val key = call.cacheKey()
        val serialized = jsonSerializer.encodeToString(serializationStrategy, response)
        diskCache.edit(key)?.let { editor ->
            editor.newSink(0)
                .buffer()
                .use {
                    it.writeUtf8(serialized.encrypt(HTTP_CACHE_KEY_ALIAS))
                }
            editor.commit()
        }
    }

    override fun <T> restore(
        call: Call<T>,
        deserializationStrategy: DeserializationStrategy<T>
    ): T? {
        val key = call.cacheKey()

        val restored = diskCache[key]?.let { snapshot ->
            snapshot.use {
                it.getSource(0).buffer().readUtf8()
            }
        }?.decrypt(HTTP_CACHE_KEY_ALIAS)

        with(Timber.tag(TAG)) {
            i("--> [CACHE] ${call.request().method} - ${call.request().url}]")
            i("<-- $restored")
        }

        return restored?.let { saved ->
            jsonSerializer.decodeFromString(
                deserializationStrategy,
                saved
            )
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
        private const val HTTP_CACHE_KEY_ALIAS = "HttpCache"

        private const val CACHE_VERSION = 1
        private const val MAX_VALUES_PER_KEY = 1
        const val DEFAULT_CACHE_MAX_SIZE = 5L * 1024 * 1024
    }
}
