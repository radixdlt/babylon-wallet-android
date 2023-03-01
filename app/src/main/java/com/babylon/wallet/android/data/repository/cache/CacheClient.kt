package com.babylon.wallet.android.data.repository.cache

import android.content.Context
import java.io.File
import javax.inject.Inject
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.concurrent.TaskRunner
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import rdx.works.core.decrypt
import rdx.works.core.encrypt

interface CacheClient {

    fun write(key: String, value: String)

    fun read(key: String): String?

}

class EncryptedDiskCacheClient @Inject constructor(
    applicationContext: Context,
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

    override fun write(key: String, value: String) {
        diskCache.edit(key)?.let { editor ->
            editor.newSink(0)
                .buffer()
                .use {
                    it.writeUtf8(value.encrypt(HTTP_CACHE_KEY_ALIAS))
                }
            editor.commit()
        }
    }

    override fun read(key: String): String? = diskCache[key]?.let { snapshot ->
        snapshot.use {
            it.getSource(0).buffer().readUtf8()
        }
    }?.decrypt(HTTP_CACHE_KEY_ALIAS)

    companion object {
        private const val MAX_VALUES_PER_KEY = 1
        private const val DEFAULT_CACHE_MAX_SIZE = 5L * 1024 * 1024
        private const val HTTP_CACHE_KEY_ALIAS = "HttpCache"

        private const val HTTP_CACHE_FOLDER = "http_cache"
        private const val CACHE_VERSION = 1
    }

}
