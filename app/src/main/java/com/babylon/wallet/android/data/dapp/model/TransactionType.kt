package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.ThirdPartyDeposits

sealed interface TransactionType {
    data class UpdateThirdPartyDeposits(val thirdPartyDeposits: ThirdPartyDeposits) : TransactionType
    data class CreateRolaKey(val factorInstance: HierarchicalDeterministicFactorInstance) : TransactionType
    data object Generic : TransactionType
}
