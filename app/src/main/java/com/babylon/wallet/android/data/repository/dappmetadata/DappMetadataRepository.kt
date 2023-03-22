package com.babylon.wallet.android.data.repository.dappmetadata

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.DynamicUrlApi
import com.babylon.wallet.android.data.gateway.model.toDomainModel
import com.babylon.wallet.android.data.repository.cache.CacheParameters
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.cache.TimeoutDuration
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.execute
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.utils.isValidHttpsUrl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DappMetadataRepository {
    suspend fun verifyDapp(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<Boolean>

    suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappMetadata>
}

class DappMetadataRepositoryImpl @Inject constructor(
    private val dynamicUrlApi: DynamicUrlApi,
    private val entityRepository: EntityRepository,
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
                ).map { gatewayMetadata ->
                    when {
                        !gatewayMetadata.isDappDefinition() -> {
                            Result.Error(
                                TransactionApprovalException(
                                    DappRequestFailure.DappVerificationFailure.WrongAccountType
                                )
                            )
                        }
                        gatewayMetadata.getRelatedDomainName() != origin -> {
                            Result.Error(
                                TransactionApprovalException(DappRequestFailure.DappVerificationFailure.UnknownWebsite)
                            )
                        }
                        else -> {
                            wellKnownFileMetadata(origin)
                        }
                    }
                }.map { wellKnownFileDappsMetadata ->
                    val isWellKnown = wellKnownFileDappsMetadata.any {
                        it.dAppDefinitionAddress == dAppDefinitionAddress
                    }
                    if (isWellKnown) {
                        Result.Success(true)
                    } else {
                        Result.Error(
                            TransactionApprovalException(
                                DappRequestFailure.DappVerificationFailure.UnknownDefinitionAddress
                            )
                        )
                    }
                }
            } else {
                Result.Error(
                    TransactionApprovalException(DappRequestFailure.DappVerificationFailure.UnknownWebsite)
                )
            }
        }
    }

    private suspend fun wellKnownFileMetadata(
        origin: String
    ): Result<List<DappMetadata>> {
        return withContext(ioDispatcher) {
            dynamicUrlApi.wellKnownDappDefinition(
                "$origin/${BuildConfig.WELL_KNOWN_URL_SUFFIX}"
            ).execute(
                cacheParameters = CacheParameters(
                    httpCache = cache,
                    timeoutDuration = TimeoutDuration.FIVE_MINUTES
                ),
                map = { response ->
                    response.dAppMetadata.map { it.toDomainModel() }
                },
                error = {
                    TransactionApprovalException(DappRequestFailure.DappVerificationFailure.RadixJsonNotFound)
                }
            )
        }
    }

    override suspend fun getDappMetadata(
        defitnionAddress: String,
        needMostRecentData: Boolean
    ): Result<DappMetadata> {
        return withContext(ioDispatcher) {
            when (
                val result = entityRepository.entityDetails(
                    address = defitnionAddress,
                    isRefreshing = needMostRecentData
                )
            ) {
                is Result.Error -> Result.Error(result.exception)
                is Result.Success -> Result.Success(
                    DappMetadata(
                        defitnionAddress,
                        result.data.metadata.items.associate { it.key to it.value.asString.toString() } // TODO 1181
                    )
                )
            }
        }
    }
}
