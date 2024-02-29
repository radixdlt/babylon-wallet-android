package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.ret.transaction.TransactionManifestData
import java.math.BigDecimal
import javax.inject.Inject

class SearchFeePayersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(manifestData: TransactionManifestData, lockFee: BigDecimal): Result<FeePayerSearchResult> {
        val allAccounts = profileUseCase.accountsOnCurrentNetwork()
        return stateRepository.getOwnedXRD(accounts = allAccounts).map { accountsWithXRD ->
            val candidates = accountsWithXRD.map { entry ->
                FeePayerSearchResult.FeePayerCandidate(
                    account = entry.key,
                    xrdAmount = entry.value
                )
            }
            val candidateAddress = manifestData.feePayerCandidates().firstOrNull { address ->
                candidates.any { it.account.address == address && it.xrdAmount >= lockFee }
            }

            FeePayerSearchResult(
                feePayerAddress = candidateAddress,
                candidates = candidates
            )
        }
    }
}
