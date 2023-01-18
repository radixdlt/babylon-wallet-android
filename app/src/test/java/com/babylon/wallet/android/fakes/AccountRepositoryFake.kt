package com.babylon.wallet.android.fakes

import com.babylon.wallet.android.mockdata.account1
import com.babylon.wallet.android.mockdata.account2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.model.pernetwork.AccountSigner
import rdx.works.profile.data.repository.AccountRepository

class AccountRepositoryFake : AccountRepository {

    private val accountsData = flowOf(
        listOf(account1, account2)
    )

    override val accounts: Flow<List<Account>> = accountsData

    override suspend fun getAccounts(): List<Account> {
        return accountsData.first()
    }

    override suspend fun getAccountByAddress(address: String): Account? {
        TODO("Not yet implemented")
    }

    override suspend fun getSignersForAddresses(networkId: Int, addresses: List<String>): List<AccountSigner> {
        TODO("Not yet implemented")
    }
}
