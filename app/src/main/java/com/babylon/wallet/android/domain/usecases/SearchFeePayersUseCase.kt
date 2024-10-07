package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.isZero
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class SearchFeePayersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(manifestData: TransactionManifestData, lockFee: Decimal192): Result<TransactionFeePayers> {
        val allAccounts = profileUseCase().activeAccountsOnCurrentNetwork
        return stateRepository.getOwnedXRD(accounts = allAccounts).map { accountsWithXRD ->
            val candidates = accountsWithXRD.mapNotNull { entry ->
                if (entry.value.isZero) return@mapNotNull null

                TransactionFeePayers.FeePayerCandidate(
                    account = entry.key,
                    xrdAmount = entry.value,
                    hasEnoughBalance = entry.value >= lockFee
                )
            }
            val candidateAddress = manifestData.feePayerCandidates().firstOrNull { address ->
                candidates.any { it.account.address == address && it.xrdAmount >= lockFee }
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

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: Decimal192, inSet: Set<Account>) = find {
    it.account in inSet && it.xrdAmount >= value
}

fun List<TransactionFeePayers.FeePayerCandidate>.findAccountWithAtLeast(value: Decimal192) = find {
    it.xrdAmount >= value
}
