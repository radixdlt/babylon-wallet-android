package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dapps.WellKnownDAppDefinitionRepository
import com.babylon.wallet.android.domain.model.DApp
import javax.inject.Inject

class GetValidatedDAppWebsiteUseCase @Inject constructor(
    private val wellKnownDAppDefinitionRepository: WellKnownDAppDefinitionRepository
) {

    suspend operator fun invoke(dApp: DApp): Result<String?> {
        val website = dApp.claimedWebsites.firstOrNull() ?: return Result.success(null)
        return wellKnownDAppDefinitionRepository.getWellKnownDAppDefinitions(website).map { dAppDefinitions ->
            if (dAppDefinitions.contains(dApp.dAppAddress)) {
                website
            } else {
                null
            }
        }
    }
}
