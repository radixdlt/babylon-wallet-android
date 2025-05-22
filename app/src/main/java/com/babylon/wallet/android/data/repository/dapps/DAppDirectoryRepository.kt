package com.babylon.wallet.android.data.repository.dapps

import com.babylon.wallet.android.data.repository.cache.database.DAppDirectoryDao
import com.babylon.wallet.android.data.repository.cache.database.DirectoryDefinitionEntity
import com.babylon.wallet.android.data.repository.toResult
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.DAppDirectory
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

    suspend fun getDirectory(isRefreshing: Boolean): Result<DAppDirectory>
}

class DAppDirectoryRepositoryImpl @Inject constructor(
    @GatewayHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val dAppDirectoryDao: DAppDirectoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DAppDirectoryRepository {

    override suspend fun getDirectory(isRefreshing: Boolean): Result<DAppDirectory> =
        withContext(ioDispatcher) {
            val cachedDirectory = dAppDirectoryDao.getDirectory(
                minValidity = directoryValidity(isRefreshing = isRefreshing)
            )

            if (cachedDirectory.isEmpty()) {
                fetchDirectory()
                    .onSuccess { directory ->
                        dAppDirectoryDao.resetDirectory()

                        val synced = InstantGenerator()
                        val dAppEntities = directory.highlighted.orEmpty().map {
                            DirectoryDefinitionEntity.from(
                                definition = it,
                                isHighlighted = true,
                                synced = synced
                            )
                        } + directory.others.orEmpty().map {
                            DirectoryDefinitionEntity.from(
                                definition = it,
                                isHighlighted = false,
                                synced = synced
                            )
                        }

                        dAppDirectoryDao.insertDirectory(directory = dAppEntities)
                    }
            } else {
                val highlightedDApps = mutableListOf<DirectoryDefinition>()
                val otherDApps = mutableListOf<DirectoryDefinition>()

                cachedDirectory.onEach { dApp ->
                    if (dApp.isHighlighted) {
                        highlightedDApps.add(dApp.toDirectoryDefinition())
                    } else {
                        otherDApps.add(dApp.toDirectoryDefinition())
                    }
                }

                Result.success(
                    DAppDirectory(
                        highlighted = highlightedDApps,
                        others = otherDApps
                    )
                )
            }
        }

    private suspend fun fetchDirectory(): Result<DAppDirectory> =
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
