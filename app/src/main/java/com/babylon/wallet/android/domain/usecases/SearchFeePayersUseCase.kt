package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.data.transaction.model.TransactionFeePayers
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.TransactionManifestData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class SearchFeePayersUseCase @Inject constructor(
    private val profileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(manifestData: TransactionManifestData, lockFee: Decimal192): Result<TransactionFeePayers> {
        val allAccounts = profileUseCase.accountsOnCurrentNetwork()
        return stateRepository.getOwnedXRD(accounts = allAccounts).map { accountsWithXRD ->
            val candidates = accountsWithXRD.map { entry ->
                TransactionFeePayers.FeePayerCandidate(
                    account = entry.key,
                    xrdAmount = entry.value
                )
            }
            val candidateAddress = manifestData.feePayerCandidates().firstOrNull { address ->
                candidates.any { it.account.address == address.string && it.xrdAmount >= lockFee }
            }

            TransactionFeePayers(
                selectedAccountAddress = candidateAddress,
                candidates = candidates
            )
        }
    }
}
