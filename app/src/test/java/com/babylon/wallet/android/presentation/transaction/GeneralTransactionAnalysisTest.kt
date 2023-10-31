package com.babylon.wallet.android.presentation.transaction

import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.transaction.analysis.sort
import com.radixdlt.ret.ResourceTracker
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeneralTransactionAnalysisTest {

    @Test
    fun `when deposit accounts are in the same order, order is retained`() = runTest {
        val accountsDeposits = mapOf(
            "rdx_t_1" to listOf<ResourceTracker>(),
            "rdx_t_2" to listOf(),
        )
        val allAccounts = listOf(
            SampleDataProvider().sampleAccount(
                address = "rdx_t_1",
                name = "One account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_2",
                name = "Two account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_3",
                name = "Three account"
            )
        )

        val sortedAccountsDeposits = accountsDeposits.sort(allAccounts)
        assert(sortedAccountsDeposits.keys.toList()[0] == allAccounts[0].address)
        assert(sortedAccountsDeposits.keys.toList()[1] == allAccounts[1].address)
    }

    @Test
    fun `when deposit accounts are not in the same order, order is taken after owned accounts`() = runTest {
        val accountsDeposits = mapOf(
            "rdx_t_2" to listOf<ResourceTracker>(),
            "rdx_t_1" to listOf(),
        )
        val allAccounts = listOf(
            SampleDataProvider().sampleAccount(
                address = "rdx_t_1",
                name = "One account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_2",
                name = "Two account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_3",
                name = "Three account"
            )
        )

        val sortedAccountsDeposits = accountsDeposits.sort(allAccounts)
        assert(sortedAccountsDeposits.keys.toList()[0] == allAccounts[0].address)
        assert(sortedAccountsDeposits.keys.toList()[1] == allAccounts[1].address)
    }

    @Test
    fun `when deposit accounts are not in the same order and have third party accounts, owned accounts shown first `() = runTest {
        val accountsDeposits = mapOf(
            "rdx_t_2" to listOf<ResourceTracker>(),
            "rdx_t_1" to listOf(),
            "rdx_t_third_party1" to listOf(),
            "rdx_t_third_party2" to listOf()
        )
        val allAccounts = listOf(
            SampleDataProvider().sampleAccount(
                address = "rdx_t_1",
                name = "One account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_2",
                name = "Two account"
            ),
            SampleDataProvider().sampleAccount(
                address = "rdx_t_3",
                name = "Three account"
            )
        )

        val sortedAccountsDeposits = accountsDeposits.sort(allAccounts)
        assert(sortedAccountsDeposits.keys.toList()[0] == allAccounts[0].address)
        assert(sortedAccountsDeposits.keys.toList()[1] == allAccounts[1].address)
        assert(sortedAccountsDeposits.keys.toList()[2] != allAccounts[2].address)
    }
}