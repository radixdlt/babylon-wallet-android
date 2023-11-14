package com.babylon.wallet.android.data.repository.dappmetadata

import com.babylon.wallet.android.data.gateway.apis.DAppDefinitionApi
import com.babylon.wallet.android.data.gateway.apis.StateApi
import com.babylon.wallet.android.data.gateway.extensions.asMetadataItems
import com.babylon.wallet.android.data.gateway.extensions.divisibility
import com.babylon.wallet.android.data.gateway.extensions.extractBehaviours
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
import com.babylon.wallet.android.di.JsonConverterFactory
import com.babylon.wallet.android.di.SimpleHttpClient
import com.babylon.wallet.android.di.buildApi
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.presentation.model.ActionableAddress
import com.babylon.wallet.android.utils.isValidHttpsUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Converter
import javax.inject.Inject

interface DAppRepository {
    suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String, wellKnownFileCheck: Boolean = true): Result<Boolean>

    suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forDapp,
        needMostRecentData: Boolean
    ): Result<DApp>

    suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey> = ExplicitMetadataKey.forDapp,
        needMostRecentData: Boolean
    ): Result<List<DApp>>

    suspend fun getDAppResources(
        dAppMetadata: DApp,
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
        dAppDefinitionAddress: String,
        wellKnownFileCheck: Boolean
    ): Result<Boolean> = withContext(ioDispatcher) {
        if (origin.isValidHttpsUrl()) {
            getDAppMetadata(
                definitionAddress = dAppDefinitionAddress,
                explicitMetadata = ExplicitMetadataKey.forDapp,
                needMostRecentData = true
            ).mapCatching { gatewayMetadata ->
                when {
                    !gatewayMetadata.isDappDefinition -> {
                        throw RadixWalletException.DappVerificationException.WrongAccountType
                    }

                    !gatewayMetadata.isRelatedWith(origin) -> {
                        throw RadixWalletException.DappVerificationException.UnknownWebsite
                    }

                    else -> {
                        if (wellKnownFileCheck) {
                            verifyWellKnownFileContainsDappDefinitionAddress(origin, dAppDefinitionAddress).getOrThrow()
                        } else {
                            true
                        }
                    }
                }
            }
        } else {
            Result.failure(RadixWalletException.DappVerificationException.UnknownWebsite)
        }
    }

    private suspend fun verifyWellKnownFileContainsDappDefinitionAddress(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean> {
        return wellKnownFileMetadata(origin).map { wellKnownFileDAppDefinitionAddresses ->
            val isWellKnown = wellKnownFileDAppDefinitionAddresses.any { it == dAppDefinitionAddress }
            if (isWellKnown) {
                true
            } else {
                throw RadixWalletException.DappVerificationException.UnknownDefinitionAddress
            }
        }
    }

    override suspend fun getDAppMetadata(
        definitionAddress: String,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<DApp> = getDAppsMetadata(
        definitionAddresses = listOf(definitionAddress),
        explicitMetadata = explicitMetadata,
        needMostRecentData = needMostRecentData
    ).mapCatching { dAppWithMetadataItems ->
        if (dAppWithMetadataItems.isEmpty()) {
            throw RadixWalletException.DappMetadataEmpty
        } else {
            dAppWithMetadataItems.first()
        }
    }

    override suspend fun getDAppsMetadata(
        definitionAddresses: List<String>,
        explicitMetadata: Set<ExplicitMetadataKey>,
        needMostRecentData: Boolean
    ): Result<List<DApp>> = withContext(ioDispatcher) {
        if (definitionAddresses.isEmpty()) return@withContext Result.success(emptyList())

        val optIns = if (explicitMetadata.isNotEmpty()) {
            StateEntityDetailsOptIns(
                explicitMetadata = explicitMetadata.map { it.key }
            )
        } else {
            null
        }

        stateApi.stateEntityDetails(
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
                    DApp.from(
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
                map = { response -> response.dApps.map { it.dAppDefinitionAddress } },
                error = {
                    RadixWalletException.DappVerificationException.RadixJsonNotFound
                }
            )
        }
    }

    @Suppress("LongMethod")
    override suspend fun getDAppResources(
        dAppMetadata: DApp,
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

        return listOfEntityDetailsResponsesResult
            .fold(
                onSuccess = { entityDetailsResponses ->
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
                            ownedAmount = null, // No owned amount given in metadata
                            nameMetadataItem = metadataItems.consume(),
                            symbolMetadataItem = metadataItems.consume(),
                            descriptionMetadataItem = metadataItems.consume(),
                            iconUrlMetadataItem = metadataItems.consume(),
                            assetBehaviours = fungibleItem.details?.extractBehaviours(),
                            currentSupply = fungibleItem.details?.totalSupply()?.toBigDecimal(),
                            validatorMetadataItem = metadataItems.consume(),
                            poolMetadataItem = metadataItems.consume(),
                            divisibility = fungibleItem.details?.divisibility(),
                            dAppDefinitionsMetadataItem = metadataItems.consume()
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
                            assetBehaviours = nonFungibleItem.details?.extractBehaviours(),
                            currentSupply = nonFungibleItem.details?.totalSupply()?.toIntOrNull()
                        )
                    }

                    val dAppResources = DAppResources(
                        fungibleResources = fungibleResources.filter {
                            it.dappDefinitions.contains(dAppMetadata.dAppAddress)
                        },
                        nonFungibleResources = nonFungibleResource.filter {
                            it.dappDefinitions.contains(dAppMetadata.dAppAddress)
                        },
                    )

                    Result.success(dAppResources)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
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
