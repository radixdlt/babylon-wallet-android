package com.babylon.wallet.android.presentation.transaction.model

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress

sealed interface InvolvedAccount {

    val address: AccountAddress

    data class Owned(val account: Account): InvolvedAccount {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(override val address: AccountAddress): InvolvedAccount
}