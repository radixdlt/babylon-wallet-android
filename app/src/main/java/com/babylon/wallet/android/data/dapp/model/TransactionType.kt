package com.babylon.wallet.android.data.dapp.model

import rdx.works.profile.data.model.pernetwork.Network

sealed interface TransactionType {
    data class UpdateThirdPartyDeposits(val thirdPartyDeposits: Network.Account.OnLedgerSettings.ThirdPartyDeposits) : TransactionType
    object CreateRolaKey : TransactionType
    object Generic : TransactionType
}
