package com.babylon.wallet.android.presentation.transaction

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleRandom
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TransferableTests {

    private val account1 = Account.sampleMainnet()
    private val account2 = Account.sampleMainnet.other()

    @Test
    fun `test ordering of transferable accounts with assets`() {
        val ownedAccounts = listOf(account1, account2)
        val input = listOf(
            AccountWithTransferableResources.Other(
                address = AccountAddress.sampleRandom(NetworkId.MAINNET),
                resources = listOf()
            ),
            AccountWithTransferableResources.Other(
                address = AccountAddress.sampleRandom(NetworkId.MAINNET),
                resources = listOf()
            ),
            AccountWithTransferableResources.Owned(
                ownedAccounts[1],
                resources = listOf()
            ),
            AccountWithTransferableResources.Owned(
                ownedAccounts[0],
                resources = listOf()
            )
        )

        val output = input.sortedWith(AccountWithTransferableResources.Companion.Sorter(ownedAccounts))

        assertEquals(
            listOf(
                AccountWithTransferableResources.Owned(
                    ownedAccounts[0],
                    resources = listOf()
                ),
                AccountWithTransferableResources.Owned(
                    ownedAccounts[1],
                    resources = listOf()
                ),
                AccountWithTransferableResources.Other(
                    address = AccountAddress.sampleRandom(NetworkId.MAINNET),
                    resources = listOf()
                ),
                AccountWithTransferableResources.Other(
                    address = AccountAddress.sampleRandom(NetworkId.MAINNET),
                    resources = listOf()
                )
            ),
            output
        )
    }

}