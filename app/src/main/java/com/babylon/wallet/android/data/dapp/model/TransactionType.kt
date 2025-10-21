package com.babylon.wallet.android.data.dapp.model

import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.ThirdPartyDeposits

sealed interface TransactionType {

    data class UpdateThirdPartyDeposits(val thirdPartyDeposits: ThirdPartyDeposits) : TransactionType

    data class DeleteAccount(val accountAddress: AccountAddress) : TransactionType

    data class SecurifyEntity(val entityAddress: AddressOfAccountOrPersona) : TransactionType

    data object InitiateSecurityStructureRecovery : TransactionType

    data class ConfirmSecurityStructureRecovery(val entityAddress: AddressOfAccountOrPersona) : TransactionType

    data class StopSecurityStructureRecovery(val entityAddress: AddressOfAccountOrPersona) : TransactionType

    data object Generic : TransactionType
}
