package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.data.repository.cache.database.DAppDirectoryDao
import com.babylon.wallet.android.data.repository.cache.database.DirectoryDefinitionEntity
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import rdx.works.core.InstantGenerator
import rdx.works.core.di.GatewayHttpClient
import retrofit2.Converter
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface DAppDirectoryRepository {

    suspend fun getDirectory(isRefreshing: Boolean): Result<List<DirectoryDefinition>>
}

class DAppDirectoryRepositoryImpl @Inject constructor(
    @GatewayHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val dAppDirectoryDao: DAppDirectoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DAppDirectoryRepository {

    override suspend fun getDirectory(isRefreshing: Boolean): Result<List<DirectoryDefinition>> =
        withContext(ioDispatcher) {
            val cachedDirectory = dAppDirectoryDao.getDirectory(
                minValidity = directoryValidity(isRefreshing = isRefreshing)
            )

            if (cachedDirectory.isEmpty()) {
                fetchDirectory()
                    .onSuccess { directory ->
                        val synced = InstantGenerator()
                        dAppDirectoryDao.insertDirectory(
                            directory = directory.map {
                                DirectoryDefinitionEntity.from(definition = it, synced = synced)
                            }
                        )
                    }
            } else {
                Result.success(cachedDirectory.map { it.toDirectoryDefinition() })
            }
        }

    private suspend fun fetchDirectory(): Result<List<DirectoryDefinition>> =
        buildApi<DAppDirectoryApi>(
            baseUrl = BASE_URL,
            okHttpClient = okHttpClient,
            jsonConverterFactory = jsonConverterFactory
        ).directory().toResult()

    companion object {
        private const val BASE_URL = "https://dapps-list.radixdlt.com"
        private val directoryCacheDuration = 24.toDuration(DurationUnit.HOURS)

        fun directoryValidity(isRefreshing: Boolean = false) =
            InstantGenerator().toEpochMilli() - if (isRefreshing) 0 else directoryCacheDuration.inWholeMilliseconds
    }
}
