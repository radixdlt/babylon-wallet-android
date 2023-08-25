package com.babylon.wallet.android.data.repository.cache

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.concurrent.TaskRunner
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import rdx.works.core.KeySpec
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import timber.log.Timber
import java.io.File

interface CacheClient {

    fun <T> write(key: String, value: CachedValue<T>, serializer: KSerializer<T>)

    fun <T> read(key: String, serializer: KSerializer<T>): CachedValue<T>?

    fun invalidate()
}

class EncryptedDiskCacheClient(
    applicationContext: Context,
    private val jsonSerializer: Json,
    cacheFolderName: String = HTTP_CACHE_FOLDER,
    cacheVersion: Int = CACHE_VERSION
) : CacheClient {

    private val diskCache: DiskLruCache = DiskLruCache(
        fileSystem = FileSystem.SYSTEM,
        directory = File(applicationContext.cacheDir, cacheFolderName)
            .apply { mkdir() }
            .toOkioPath(),
        appVersion = cacheVersion,
        valueCount = MAX_VALUES_PER_KEY,
        maxSize = DEFAULT_CACHE_MAX_SIZE,
        taskRunner = TaskRunner.INSTANCE
    )

    private val logger = Timber.tag(TAG)

    override fun <T> write(key: String, value: CachedValue<T>, serializer: KSerializer<T>) {
        val serialized = jsonSerializer.encodeToString(
            CachedValueSerializer(serializer),
            value
        )

        diskCache.edit(key)?.let { editor ->
            editor.newSink(0)
                .buffer()
                .use {
                    it.writeUtf8(serialized.encrypt(KeySpec.Cache(HTTP_CACHE_KEY_ALIAS)).getOrThrow())
                }
            editor.commit()
        }
    }

    @Suppress("SwallowedException")
    override fun <T> read(key: String, serializer: KSerializer<T>): CachedValue<T>? {
        val restored = diskCache[key]?.let { snapshot ->
            snapshot.use {
                it.getSource(0).buffer().readUtf8()
            }
        }?.decrypt(KeySpec.Cache(HTTP_CACHE_KEY_ALIAS))?.getOrNull()

        return restored?.let { saved ->
            try {
                jsonSerializer.decodeFromString(
                    CachedValueSerializer(serializer),
                    saved
                )
            } catch (exception: IllegalArgumentException) {
                logger
                    .w("The value extracted belongs to a previous schema, cache value is considered stale")
                null
            }
        }?.also {
            logger.d("    [CACHE] $restored")
        }
    }

    override fun invalidate() {
        diskCache.evictAll()
    }

    companion object {
        private const val TAG = "HTTP_CACHE"
        private const val MAX_VALUES_PER_KEY = 1
        private const val DEFAULT_CACHE_MAX_SIZE = 5L * 1024 * 1024
        private const val HTTP_CACHE_KEY_ALIAS = "HttpCache"

        private const val HTTP_CACHE_FOLDER = "http_cache"
        private const val CACHE_VERSION = 1
    }
}
