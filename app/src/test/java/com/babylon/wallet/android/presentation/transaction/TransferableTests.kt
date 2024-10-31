package com.babylon.wallet.android.presentation.transaction

import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import com.radixdlt.sargon.samples.sampleRandom
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TransferableTests {

    private val account1 = Account.sampleMainnet()
    private val account2 = Account.sampleMainnet.other()
    private val other = List(2) {
        AccountAddress.sampleRandom(NetworkId.MAINNET)
    }.sortedBy { it.string }

    @Test
    fun `test ordering of transferable accounts with assets`() {
        val ownedAccounts = listOf(account1, account2)
        val input = listOf(
            AccountWithTransferables.Other(
                address = other[1],
                transferables = listOf()
            ),
            AccountWithTransferables.Other(
                address = other[0],
                transferables = listOf()
            ),
            AccountWithTransferables.Owned(
                ownedAccounts[1],
                transferables = listOf()
            ),
            AccountWithTransferables.Owned(
                ownedAccounts[0],
                transferables = listOf()
            )
        )

        val output = input.sortedWith(AccountWithTransferables.Companion.Sorter(ownedAccounts))

        assertEquals(
            listOf(
                AccountWithTransferables.Owned(
                    ownedAccounts[0],
                    transferables = listOf()
                ),
                AccountWithTransferables.Owned(
                    ownedAccounts[1],
                    transferables = listOf()
                ),
                AccountWithTransferables.Other(
                    address = other[1],
                    transferables = listOf()
                ),
                AccountWithTransferables.Other(
                    address = other[0],
                    transferables = listOf()
                )
            ),
            output
        )
    }

}