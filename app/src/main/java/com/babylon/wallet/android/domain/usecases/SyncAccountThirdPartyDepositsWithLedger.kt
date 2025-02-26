package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.ResolveAccountsLedgerStateRepository
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.first
import rdx.works.core.sargon.updateThirdPartyDepositSettings
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.data.repository.profile
import javax.inject.Inject

class SyncAccountThirdPartyDepositsWithLedger @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val resolveAccountsLedgerStateRepository: ResolveAccountsLedgerStateRepository
) {

    suspend operator fun invoke(
        account: Account
    ) {
        val accountOnLedger = resolveAccountsLedgerStateRepository(
            accounts = listOf(account)
        ).getOrNull()?.firstOrNull()?.account ?: return

        if (accountOnLedger.onLedgerSettings.thirdPartyDeposits != account.onLedgerSettings.thirdPartyDeposits) {
            val profile = profileRepository.profile.first()
            val updatedProfile = profile.updateThirdPartyDepositSettings(
                account = account,
                thirdPartyDeposits = accountOnLedger.onLedgerSettings.thirdPartyDeposits
            )

            profileRepository.saveProfile(updatedProfile)
        }
    }
}
