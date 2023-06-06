package com.babylon.wallet.android.data.repository.dappmetadata

import androidx.core.net.toUri
import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.asMetadataStringMap
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
import com.babylon.wallet.android.domain.common.switchMap
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
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
    ).map { dAppWithMetadataItems ->
        dAppWithMetadataItems.first()
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DAppWithMetadata>> = withContext(ioDispatcher) {
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
                val metadataMap = fungibleItem.metadata.asMetadataStringMap()
                Resource.FungibleResource(
                    resourceAddress = fungibleItem.address,
                    amount = null, // No amount given in metadata
                    nameMetadataItem = metadataMap[ExplicitMetadataKey.NAME.key]?.let { NameMetadataItem(it) },
                    symbolMetadataItem = metadataMap[ExplicitMetadataKey.SYMBOL.key]?.let { SymbolMetadataItem(it) },
                    descriptionMetadataItem = metadataMap[ExplicitMetadataKey.DESCRIPTION.key]?.let { DescriptionMetadataItem(it) },
                    iconUrlMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let { IconUrlMetadataItem(it.toUri()) }
                )
            }

            val nonFungibleResource = nonFungibleItems.map { nonFungibleItem ->
                val metadataMap = nonFungibleItem.metadata.asMetadataStringMap()

                Resource.NonFungibleResource.Item(
                    collectionAddress = nonFungibleItem.address,
                    localId = Resource.NonFungibleResource.Item.ID.from(
                        nonFungibleItem.ancestorIdentities?.globalAddress.orEmpty()
                    ),
                    iconMetadataItem = metadataMap[ExplicitMetadataKey.ICON_URL.key]?.let {
                        IconUrlMetadataItem(it.toUri())
                    }
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
