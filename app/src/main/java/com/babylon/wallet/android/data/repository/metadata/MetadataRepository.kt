package com.babylon.wallet.android.data.repository.metadata

import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem
import javax.inject.Inject

interface MetadataRepository {

    suspend fun getResourcesMetadata(
        resourceAddresses: List<String>,
        isRefreshing: Boolean = true
    ): Result<Map<String, List<MetadataItem>>>
}

class MetadataRepositoryImpl @Inject constructor(
    private val stateApi: StateApi,
    private val cache: HttpCache
) : MetadataRepository {

    override suspend fun getResourcesMetadata(
        resourceAddresses: List<String>,
        isRefreshing: Boolean
    ): Result<Map<String, List<MetadataItem>>> = getStateEntityDetailsResponse(
        addresses = resourceAddresses,
        explicitMetadata = ExplicitMetadataKey.forAssets,
        isRefreshing = isRefreshing
    ).fold(
        onSuccess = { entityDetailsResponses ->
            Result.success(buildMapOfResourceAddressesWithMetadata(entityDetailsResponses))
        },
        onFailure = {
            Result.failure(it)
        }
    )

    private fun buildMapOfResourceAddressesWithMetadata(
        entityDetailsResponses: List<StateEntityDetailsResponse>
    ): Map<String, List<MetadataItem>> {
        return entityDetailsResponses.map { entityDetailsResponse ->
            entityDetailsResponse.items
                .groupingBy { entityDetailsResponseItem ->
                    entityDetailsResponseItem.address
                }
                .foldTo(mutableMapOf(), listOf<MetadataItem>()) { _, entityItem ->
                    entityItem.metadata.asMetadataItems()
                }
        }.flatMap { map ->
            map.asSequence()
        }.associate { map ->
            map.key to map.value
        }
    }

    private suspend fun getStateEntityDetailsResponse(
        addresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        isRefreshing: Boolean
    ): Result<List<StateEntityDetailsResponse>> {
        val responses = addresses
            .chunked(EntityRepositoryImpl.CHUNK_SIZE_OF_ITEMS)
            .map { chunkedAddresses ->
                stateApi.stateEntityDetails(
                    StateEntityDetailsRequest(
                        addresses = chunkedAddresses,
                        aggregationLevel = ResourceAggregationLevel.vault,
                        optIns = StateEntityDetailsOptIns(
                            explicitMetadata = explicitMetadata.map { it.key }
                        )
                    )
                ).execute(
                    cacheParameters = CacheParameters(
                        httpCache = cache,
                        timeoutDuration = if (isRefreshing) TimeoutDuration.NO_CACHE else TimeoutDuration.ONE_MINUTE
                    ),
                    map = {
                        it
                    }
                )
            }

        // if you find any error response in the list of StateEntityDetailsResponses then return error
        return if (responses.any { response -> response.isFailure }) {
            val errorResponse = responses.first { response -> response.isFailure }.map {
                listOf(it)
            }
            errorResponse
        } else { // otherwise all StateEntityDetailsResponses are success so return the list
            Result.success(responses.mapNotNull { it.getOrNull() })
        }
    }
}
