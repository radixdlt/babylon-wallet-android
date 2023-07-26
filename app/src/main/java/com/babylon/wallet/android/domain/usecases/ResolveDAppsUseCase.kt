package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.asKotlinResult
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import rdx.works.core.then
import javax.inject.Inject

class ResolveDAppsUseCase @Inject constructor(
    private val dAppRepository: DAppRepository,
    private val getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase
) {

    suspend operator fun invoke(
        componentAddress: String
    ): Result<DAppWithMetadataAndAssociatedResources> {
        return dAppRepository.getDAppMetadata(
            definitionAddress = componentAddress,
            needMostRecentData = true
        ).asKotlinResult().then { componentWithMetadata ->
            val dAppDefinitionAddress = componentWithMetadata.definitionAddresses.firstOrNull()
            if (dAppDefinitionAddress != null) {
                val result = getDAppWithMetadataAndAssociatedResourcesUseCase(
                    definitionAddress = dAppDefinitionAddress,
                    needMostRecentData = true
                )

                result.asKotlinResult()
            } else {
                Result.failure(DappRequestFailure.DappVerificationFailure.WrongAccountType)
            }
        }
    }
}
