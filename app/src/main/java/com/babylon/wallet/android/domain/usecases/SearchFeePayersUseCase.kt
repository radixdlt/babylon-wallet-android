package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.radixdlt.ret.ManifestSummary
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import java.math.BigDecimal
import javax.inject.Inject

class SearchFeePayersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(manifestSummary: ManifestSummary, lockFee: BigDecimal): Result<FeePayerSearchResult> {
        val allAccounts = profileUseCase.accountsOnCurrentNetwork()
        return stateRepository.getOwnedXRD(accounts = allAccounts).map { accountsWithXRD ->
            val candidates = accountsWithXRD.map { entry ->
                FeePayerSearchResult.FeePayerCandidate(
                    account = entry.key,
                    xrdAmount = entry.value
                )
            }
            val addresses =
                manifestSummary.accountsWithdrawnFrom + manifestSummary.accountsDepositedInto + manifestSummary.accountsRequiringAuth
            val candidateAddress = addresses.map { it.addressString() }.firstOrNull { address ->
                candidates.any { it.account.address == address && it.xrdAmount >= lockFee }
            }

            FeePayerSearchResult(
                feePayerAddress = candidateAddress,
                candidates = candidates
            )
        }
    }
}
