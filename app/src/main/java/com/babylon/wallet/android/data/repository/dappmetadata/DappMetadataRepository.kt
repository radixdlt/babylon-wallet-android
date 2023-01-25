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
    suspend fun getWellKnownDappMetadata(origin: String, dAppDefinitionAddress: String): Result<DappMetadata?>
    suspend fun validateDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean>
}

class DappMetadataRepositoryImpl @Inject constructor(
    private val dynamicUrlApi: DynamicUrlApi,
    private val entityRepository: EntityRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DappMetadataRepository {

    override suspend fun getWellKnownDappMetadata(
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
                    response.map { it.toDomainModel() }
                        .firstOrNull { it.dAppDefinitionAddress == dAppDefinitionAddress }
                }
            ).map { metadata ->
                if (metadata != null) {
                    when (
                        val result = entityRepository.entityDetails(
                            metadata.dAppDefinitionAddress
                        )
                    ) {
                        is Result.Error -> Result.Error(result.exception)
                        is Result.Success -> Result.Success(
                            metadata.copy(metadata = result.data.metadata.items.associate { it.key to it.value })
                        )
                    }
                } else {
                    Result.Error()
                }
            }
        }
    }

    override suspend fun validateDapp(origin: String, dAppDefinitionAddress: String): Result<Boolean> {
        return withContext(ioDispatcher) {
            performHttpRequest(
                call = {
                    dynamicUrlApi.wellKnownDappDefinition(
                        "$origin/${BuildConfig.WELL_KNOWN_URL_SUFFIX}"
                    )
                },
                map = { response ->
                    response.any { it.dAppDefinitionAddress == dAppDefinitionAddress }
                }
            )
        }
    }
}
