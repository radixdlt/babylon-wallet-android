package com.babylon.wallet.android.data.dapp.model

import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network

sealed interface TransactionType {
    data class UpdateThirdPartyDeposits(val thirdPartyDeposits: Network.Account.OnLedgerSettings.ThirdPartyDeposits) : TransactionType
    data class CreateRolaKey(val factorInstance: FactorInstance) : TransactionType
    object Generic : TransactionType
}
