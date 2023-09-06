package com.babylon.wallet.android.data.repository.dappmetadata

import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.calculateResourceBehaviours
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.totalSupply
import com.babylon.wallet.android.data.gateway.generated.models.ResourceAggregationLevel
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsOptIns
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsRequest
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponse
import com.babylon.wallet.android.data.gateway.generated.models.StateEntityDetailsResponseItemDetailsType
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.entity.EntityRepositoryImpl
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.common.mapIfNotEmpty
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.presentation.model.ActionableAddress
import com.babylon.wallet.android.utils.isValidHttpsUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface DAppRepository {
    suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean>

    suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forDapp,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadata>

    suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forDapp,
        needMostRecentData: Boolean
    ): Result<List<DAppWithMetadata>>

    suspend fun getDAppResources(
        dAppMetadata: DAppWithMetadata,
        isRefreshing: Boolean = true
    ): Result<DAppResources>
}

class DAppRepositoryImpl @Inject constructor(
    @SimpleHttpClient private val okHttpClient: OkHttpClient,
    @JsonConverterFactory private val jsonConverterFactory: Converter.Factory,
    private val stateApi: StateApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cache: HttpCache
) : DAppRepository {

    override suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> = withContext(ioDispatcher) {
        if (origin.isValidHttpsUrl()) {
            getDAppMetadata(
                definitionAddress = dAppDefinitionAddress,
                explicitMetadata = ExplicitMetadataKey.forDapp,
                needMostRecentData = false
            ).switchMap { gatewayMetadata ->
                when {
                    !gatewayMetadata.isDappDefinition -> {
                        Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.WrongAccountType))
                    }

                    !gatewayMetadata.isRelatedWith(origin) -> {
                        Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite))
                    }

                    else -> wellKnownFileMetadata(origin)
                }
            }.switchMap { wellKnownFileDAppDefinitions ->
                val isWellKnown = wellKnownFileDAppDefinitions.any { it == dAppDefinitionAddress }
                if (isWellKnown) {
                    Result.Success(true)
                } else {
                    Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownDefinitionAddress))
                }
            }
        } else {
            Result.Error(DappRequestException(DappRequestFailure.DappVerificationFailure.UnknownWebsite))
        }
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DAppWithMetadata> = getDAppsMetadata(
        definitionAddresses = listOf(definitionAddress),
        explicitMetadata = explicitMetadata,
        needMostRecentData = needMostRecentData
    ).mapIfNotEmpty { dAppWithMetadataItems ->
        dAppWithMetadataItems.first()
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DAppWithMetadata>> = withContext(ioDispatcher) {
        if (definitionAddresses.isEmpty()) return@withContext Result.Success(emptyList())

        val optIns = if (explicitMetadata.isNotEmpty()) {
            StateEntityDetailsOptIns(
                explicitMetadata = explicitMetadata.map { it.key }
            )
        } else {
            null
        }

        stateApi.entityDetails(
            StateEntityDetailsRequest(
                addresses = definitionAddresses,
                optIns = optIns
            )
        ).execute(
            cacheParameters = CacheParameters(
                httpCache = cache,
                timeoutDuration = if (needMostRecentData) TimeoutDuration.NO_CACHE else TimeoutDuration.FIVE_MINUTES
            ),
            map = { response ->
                response.items.map { dAppResponse ->
                    DAppWithMetadata.from(
                        address = dAppResponse.address,
                        metadataItems = dAppResponse.metadata.asMetadataItems()
                    )
                }
            },
        )
    }

    private suspend fun wellKnownFileMetadata(
        origin: String
    ): Result<List<String>> {
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
                map = { response -> response.dApps.map { it.dAppDefinitionAddress } },
                error = {
                    DappRequestException(DappRequestFailure.DappVerificationFailure.RadixJsonNotFound)
                }
            )
        }
    }

    override suspend fun getDAppResources(
        dAppMetadata: DAppWithMetadata,
        isRefreshing: Boolean
    ): Result<DAppResources> {
        val claimedResources = dAppMetadata.claimedEntities.filter {
            ActionableAddress.Type.from(it) == ActionableAddress.Type.RESOURCE
        }

        val listOfEntityDetailsResponsesResult = getStateEntityDetailsResponse(
            addresses = claimedResources,
            explicitMetadata = ExplicitMetadataKey.forAssets,
            isRefreshing = isRefreshing
        )

        return listOfEntityDetailsResponsesResult.switchMap { entityDetailsResponses ->
            val allResources = entityDetailsResponses.map {
                it.items
            }.flatten()

            val fungibleItems = allResources.filter {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.fungibleResource
            }
            val nonFungibleItems = allResources.filter {
                it.details?.type == StateEntityDetailsResponseItemDetailsType.nonFungibleResource
            }

            val fungibleResources = fungibleItems.map { fungibleItem ->
                val metadataItems = fungibleItem.metadata.asMetadataItems().toMutableList()
                Resource.FungibleResource(
                    resourceAddress = fungibleItem.address,
                    amount = null, // No amount given in metadata
                    nameMetadataItem = metadataItems.consume(),
                    symbolMetadataItem = metadataItems.consume(),
                    descriptionMetadataItem = metadataItems.consume(),
                    iconUrlMetadataItem = metadataItems.consume(),
                    behaviours = fungibleItem.details?.calculateResourceBehaviours().orEmpty(),
                    currentSupply = fungibleItem.details?.totalSupply()?.toBigDecimal(),
                    validatorMetadataItem = metadataItems.toMutableList().consume(),
                    poolMetadataItem = metadataItems.toMutableList().consume(),
                    divisibility = fungibleItem.details?.divisibility()
                )
            }

            val nonFungibleResource = nonFungibleItems.map { nonFungibleItem ->
                val metadataItems = nonFungibleItem.metadata.asMetadataItems().toMutableList()

                Resource.NonFungibleResource(
                    resourceAddress = nonFungibleItem.address,
                    amount = 0L,
                    nameMetadataItem = metadataItems.consume(),
                    descriptionMetadataItem = metadataItems.consume(),
                    iconMetadataItem = metadataItems.consume(),
                    tagsMetadataItem = metadataItems.consume(),
                    validatorMetadataItem = metadataItems.consume(),
                    items = emptyList(),
                    behaviours = nonFungibleItem.details?.calculateResourceBehaviours().orEmpty(),
                    currentSupply = nonFungibleItem.details?.totalSupply()?.toBigDecimal()
                )
            }

            val dAppsWithResources = DAppResources(
                fungibleResources = fungibleResources,
                nonFungibleResources = nonFungibleResource,
            )

            Result.Success(dAppsWithResources)
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
                stateApi.entityDetails(
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
        return if (responses.any { response -> response is Result.Error }) {
            val errorResponse = responses.first { response -> response is Result.Error }.map {
                listOf(it)
            }
            errorResponse
        } else { // otherwise all StateEntityDetailsResponses are success so return the list
            Result.Success(responses.mapNotNull { it.value() })
        }
    }
}
