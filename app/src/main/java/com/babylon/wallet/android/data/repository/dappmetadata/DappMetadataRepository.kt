package com.babylon.wallet.android.data.repository.dappmetadata

import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.gateway.model.toDomainModel
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.utils.isValidHttpsUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface DappMetadataRepository {
    suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean>

    suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappWithMetadata>

    suspend fun getDappsMetadata(
        defitnionAddresses: List<String>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>>

    suspend fun getMetadataFor(
        dAppDefinitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>>
}

class DappMetadataRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val stateApi: StateApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cache: HttpCache
) : DappMetadataRepository {

    override suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> {
        return withContext(ioDispatcher) {
            if (origin.isValidHttpsUrl()) {
                getDappMetadata(
                    defitnionAddress = dAppDefinitionAddress,
                    needMostRecentData = false
                ).switchMap { gatewayMetadata ->
                    when {
                        !gatewayMetadata.isDappDefinition -> {
                            Result.Error(
                                DappRequestException(
                                    DappRequestFailure.DappVerificationFailure.WrongAccountType
                                )
                            )
                        }
                        !gatewayMetadata.isRelatedWith(origin) -> {
                            Result.Error(
                                DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite)
                            )
                        }
                        else -> {
                            wellKnownFileMetadata(origin)
                        }
                    }
                }.switchMap { wellKnownFileDappsMetadata ->
                    val isWellKnown = wellKnownFileDappsMetadata.any {
                        it.dAppDefinitionAddress == dAppDefinitionAddress
                    }
                    if (isWellKnown) {
                        Result.Success(true)
                    } else {
                        Result.Error(
                            DappRequestException(
                                DappRequestFailure.DappVerificationFailure.UnknownDefinitionAddress
                            )
                        )
                    }
                }
            } else {
                Result.Error(
                    DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite)
                )
            }
        }
    }

    private suspend fun wellKnownFileMetadata(
        origin: String
    ): Result<List<DappWithMetadata>> {
        return withContext(ioDispatcher) {
            buildApi<DAppDefinitionApi>(
                baseUrl = origin,
                okHttpClient = okHttpClient,
                jsonConverterFactory = jsonConverterFactory
            ).wellKnownDAppDefinition().execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = TimeoutDuration.FIVE_MINUTES
                ),
                map = { response ->
                    response.dAppMetadata.map { it.toDomainModel() }
                },
                error = {
                    DappRequestException(DappRequestFailure.DappVerificationFailure.RadixJsonNotFound)
                }
            )
        }
    }

    override suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappWithMetadata> {
        return Result.Success(DappWithMetadata(""))
//        return withContext(ioDispatcher) {
//            entityRepository.stateEntityDetails(
//                addresses = listOf(defitnionAddress),
//                isRefreshing = needMostRecentData
//            ).map { response ->
//                DappWithMetadata(
//                    dAppDefinitionAddress = defitnionAddress,
//                    metadata = response.items.first().metadata.asMetadataStringMap()
//                )
//            }
//        }
    }

    override suspend fun getDappsMetadata(
        defitnionAddresses: List<String>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>> {
        return Result.Success(listOf())
//        return withContext(ioDispatcher) {
//            entityRepository.stateEntityDetails(
//                addresses = defitnionAddresses,
//                isRefreshing = needMostRecentData
//            ).map { response ->
//                response.items.filter { item ->
//                    defitnionAddresses.contains(item.address)
//                }.map {
//                    DappWithMetadata(
//                        dAppDefinitionAddress = it.address,
//                        metadata = it.metadata.asMetadataStringMap()
//                    )
//                }
//            }
//        }
    }

    override suspend fun getMetadataFor(
        dAppDefinitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DappWithMetadata>> = withContext(ioDispatcher) {
        val optIns = if (explicitMetadata.isNotEmpty()) {
            StateEntityDetailsOptIns(
                explicitMetadata = explicitMetadata.map { it.key }
            )
        } else {
            null
        }

        stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = dAppDefinitionAddresses,
                optIns = optIns
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (needMostRecentData) TimeoutDuration.NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                response.items.map { dAppResponse ->
                    DappWithMetadata.from(
                        address = dAppResponse.address,
                        metadataItems = dAppResponse.explicitMetadata?.asMetadataItems() ?: emptyList()
                    )
                }
            },
        )
    }
}
