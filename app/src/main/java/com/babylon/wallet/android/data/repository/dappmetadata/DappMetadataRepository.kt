package com.babylon.wallet.android.data.repository.dappmetadata

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.data.gateway.DynamicUrlApi
import com.babylon.wallet.android.data.gateway.model.toDomainModel
import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.data.repository.performHttpRequest
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.map
import com.babylon.wallet.android.domain.model.DappMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface DappMetadataRepository {
    suspend fun verifyDappSimple(origin: String, dAppDefinitionAddress: String): Result<Boolean>
    suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean>
    suspend fun getDappMetadata(defitnionAddress: String): Result<DappMetadata>
}

class DappMetadataRepositoryImpl @Inject constructor(
    private val dynamicUrlApi: DynamicUrlApi,
    private val entityRepository: EntityRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DappMetadataRepository {

    private suspend fun getWellKnownDappMetadata(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<DappMetadata?> {
        // TODO we need to load additional dAppDefiniton metadata as per cap-27 to do origin check
        return withContext(ioDispatcher) {
            isDappWellKnown(origin, dAppDefinitionAddress).map { metadata ->
                if (metadata != null) {
                    getDappMetadata(dAppDefinitionAddress)
                } else {
                    Result.Error()
                }
            }
        }
    }

    private suspend fun isDappWellKnown(
        origin: String,
        dAppDefinitionAddress: String
    ): Result<DappMetadata?> {
        // TODO we need to load additional dAppDefiniton metadata as per cap-27 to do origin check
        return withContext(ioDispatcher) {
            performHttpRequest(
                call = {
                    dynamicUrlApi.wellKnownDappDefinition(
                        "$origin/${BuildConfig.WELL_KNOWN_URL_SUFFIX}"
                    )
                },
                map = { response ->
                    response.dAppMetadata.map { it.toDomainModel() }
                        .firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
                }
            )
        }
    }

    override suspend fun getDappMetadata(defitnionAddress: String): Result<DappMetadata> {
        return withContext(ioDispatcher) {
            when (val result = entityRepository.entityDetails(defitnionAddress)) {
                is Result.Error -> Result.Error(result.exception)
                is Result.Success -> Result.Success(
                    DappMetadata(
                        defitnionAddress,
                        result.data.metadata.items.associate { it.key to it.value }
                    )
                )
            }
        }
    }

    override suspend fun verifyDappSimple(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return withContext(ioDispatcher) {
            isDappWellKnown(origin, dAppDefinitionAddress).map {
                Result.Success(it != null)
            }
        }
    }

    override suspend fun verifyDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return withContext(ioDispatcher) {
            getWellKnownDappMetadata(origin, dAppDefinitionAddress).map { result ->
                val dAppDomainName = result?.getRelatedDomainName()
                if (dAppDomainName != null && origin.contains(dAppDomainName)) {
                    Result.Success(true)
                } else {
                    Result.Error()
                }
            }
        }
    }
}
