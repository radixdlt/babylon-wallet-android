package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.ThirdPartyDeposits
import kotlin.time.Duration.Companion.seconds

sealed interface TransactionType {
    data class UpdateThirdPartyDeposits(val thirdPartyDeposits: ThirdPartyDeposits) : TransactionType
    data class CreateRolaKey(val factorInstance: HierarchicalDeterministicFactorInstance) : TransactionType
    data class PreAuthorized(val expiration: SubintentExpiration) : TransactionType
    data object Generic : TransactionType

    val isPreAuthorized: Boolean
        get() = this is PreAuthorized
}
