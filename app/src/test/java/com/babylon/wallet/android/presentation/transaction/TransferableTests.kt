package com.babylon.wallet.android.presentation.transaction

import com.babylon.wallet.android.mockdata.account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import junit.framework.TestCase.assertEquals
import org.junit.Test

class TransferableTests {

    @Test
    fun `test ordering of transferable accounts with assets`() {
        val ownedAccounts = listOf(
            account(
                name = "First",
                address = AccountAddress.init("account_tdx_2_12xyvakf7h0jjf4qhxy0egjmr5a6kh5ww4xd3tmvxvegu3tn9xhzjhd")
            ),
            account(
                name = "Second",
                address = AccountAddress.init("account_tdx_2_12xp0styrk298hzu2jamhw4f7uks6hlqkyzsp8flutmjj2tl8xr5n9a")
            ),
        )
        val input = listOf(
            AccountWithTransferableResources.Other(
                address = AccountAddress.init("account_tdx_2_1296p46pzdgwk3lveujxyjuszr3jw5glu2ekkdx5prh6hf9c337zruu"),
                resources = listOf()
            ),
            AccountWithTransferableResources.Other(
                address = AccountAddress.init("account_tdx_2_12yrl5dff059mtfekkjegudlcg4q2f4wvrhgz5dgfveym9kvn8lnkrq"),
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
                    address = AccountAddress.init("account_tdx_2_1296p46pzdgwk3lveujxyjuszr3jw5glu2ekkdx5prh6hf9c337zruu"),
                    resources = listOf()
                ),
                AccountWithTransferableResources.Other(
                    address = AccountAddress.init("account_tdx_2_12yrl5dff059mtfekkjegudlcg4q2f4wvrhgz5dgfveym9kvn8lnkrq"),
                    resources = listOf()
                )
            ),
            output
        )
    }

}