package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.mockdata.account
import kotlinx.coroutines.flow.flowOf
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.repository.AccountRepository

class AccountRepositoryFake : AccountRepository {

    private val accountsData = flowOf(
        listOf(account("account-1"), account("account-2"))
    )

    override suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner> {
        TODO("Not yet implemented")
    }
}
