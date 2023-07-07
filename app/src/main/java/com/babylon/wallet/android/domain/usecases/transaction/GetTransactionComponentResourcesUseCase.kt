package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.radixdlt.ret.ExecutionAnalysis
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

/**
 * This usecase gathers all accounts with its resources involved in transaction withdraws and deposits
 */
class GetTransactionComponentResourcesUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val entityRepository: EntityRepository
) {
    suspend operator fun invoke(
        executionAnalysis: ExecutionAnalysis,
    ): Result<List<AccountWithResources>> {
        val depositComponents = mutableListOf<String>()
        val withdrawComponents = mutableListOf<String>()

        executionAnalysis.transactionType

        analyzeManifestWithPreviewResponse.accountDeposits.forEach { accountDeposit ->
            depositComponents.add(
                (accountDeposit as? AccountDeposit.Predicted)?.componentAddress
                    ?: (accountDeposit as? AccountDeposit.Guaranteed)?.componentAddress.orEmpty()
            )
        }

        analyzeManifestWithPreviewResponse.accountWithdraws.forEach { accountWithdraw ->
            withdrawComponents.add(accountWithdraw.componentAddress)
        }

        val accounts = setOf(depositComponents, withdrawComponents).flatten().mapNotNull { componentAddress ->
            getProfileUseCase.accountOnCurrentNetwork(componentAddress)
        }

        return entityRepository.getAccountsWithResources(accounts)
    }
}

sealed interface ResourceRequest {
    data class Existing(val address: String) : ResourceRequest
    data class NewlyCreated(val metadata: Array<MetadataKeyValue>) : ResourceRequest
}
