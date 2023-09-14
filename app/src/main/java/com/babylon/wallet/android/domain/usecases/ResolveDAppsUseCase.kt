package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestFailure
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
        ).then { componentWithMetadata ->
            val dAppDefinitionAddress = componentWithMetadata.definitionAddresses.firstOrNull()
            if (dAppDefinitionAddress != null) {
                getDAppWithMetadataAndAssociatedResourcesUseCase(
                    definitionAddress = dAppDefinitionAddress,
                    needMostRecentData = true,
                    claimedEntityValidation = ClaimedEntityValidation.PerformFor(componentAddress)
                )
            } else {
                Result.failure(DappRequestFailure.DappVerificationFailure.WrongAccountType)
            }
        }
    }
}
