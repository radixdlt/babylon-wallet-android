package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.sumOf
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class GetAllAccountsXrdBalanceUseCase @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(): Decimal192 {
        val allAccounts = getProfileUseCase().activeAccountsOnCurrentNetwork
        return stateRepository.getOwnedXRD(accounts = allAccounts)
            .getOrNull()?.values?.sumOf { it }.orZero()
    }
}
