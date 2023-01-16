package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.AccountSlim
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

class GetAccountByAddressUseCase @Inject constructor(
    private val profileRepository: AccountRepository
) {

    suspend operator fun invoke(address: String): AccountSlim {
        val account = profileRepository.getAccount(address)
        requireNotNull(account) {
            "account is null"
        }
        return AccountSlim(
            address = account.entityAddress.address,
            appearanceID = account.appearanceID,
            displayName = account.displayName
        )
    }
}
