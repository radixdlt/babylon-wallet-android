package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import rdx.works.core.domain.DApp
import javax.inject.Inject

class GetValidatedDAppWebsiteUseCase @Inject constructor(
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(dApp: DApp): Result<String?> = withContext(ioDispatcher) {
        val validatedWebsites = dApp.claimedWebsites
            .map { website ->
                async {
                    website to wellKnownDAppDefinitionRepository
                        .getWellKnownDAppDefinitionAddresses(website)
                        .map { dAppDefinitions ->
                            dAppDefinitions.contains(dApp.dAppAddress)
                        }
                        .getOrDefault(false)
                }
            }
            .awaitAll()
            .mapNotNull { (website, validated) ->
                if (validated) website else null
            }

        Result.success(validatedWebsites.firstOrNull())
    }
}
