package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import rdx.works.core.then
import javax.inject.Inject

class ResolveDAppsUseCase @Inject constructor(
    private val dAppRepository: DAppRepository,
    private val getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase
) {

    /**
     * Each component is validated as follows:
     * - get dAppDefinitionAddress from component metadata
     * - get metadata for that address
     * - validate that account_type is "dapp definition"
     * - check if componentAddress is within claimed_entities metadata of dAppDefinitionAddress metadata
     */
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
                Result.failure(RadixWalletException.DappVerificationException.WrongAccountType)
            }
        }
    }
}
