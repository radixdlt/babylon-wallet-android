package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class SearchFeePayersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(
        feePayerCandidates: Set<AccountAddress>,
        xrdWithdrawals: Map<AccountAddress, Decimal192>,
        lockFee: Decimal192
    ): Result<TransactionFeePayers> {
        val allAccounts = profileUseCase().activeAccountsOnCurrentNetwork
        return stateRepository.getOwnedXRD(accounts = allAccounts).map { accountsWithXRD ->
            val candidates = accountsWithXRD.mapNotNull { entry ->
                if (entry.value.isZero) return@mapNotNull null
                val withdrawalAmount = xrdWithdrawals[entry.key.address].orZero()

                TransactionFeePayers.FeePayerCandidate(
                    account = entry.key,
                    xrdAmount = entry.value,
                    hasEnoughBalance = (entry.value - withdrawalAmount) >= lockFee
                )
            }
            val candidateAddress = feePayerCandidates.firstOrNull { address ->
                candidates.any { candidate ->
                    candidate.account.address == address && candidate.hasEnoughBalance
                }
            }

            TransactionFeePayers(
                selectedAccountAddress = candidateAddress,
                candidates = candidates
            )
        }
    }
}

data class TransactionFeePayers(
    val selectedAccountAddress: AccountAddress? = null,
    val candidates: List<FeePayerCandidate> = emptyList(),
) {
    data class FeePayerCandidate(
        val account: Account,
        val xrdAmount: Decimal192,
        val hasEnoughBalance: Boolean
    )
}
