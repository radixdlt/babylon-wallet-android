package com.babylon.wallet.android.presentation.transaction.model

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import rdx.works.core.mapWhen

sealed interface AccountWithTransferableResources {

    val address: AccountAddress
    val resources: List<TransferableX>

    data class Owned(
        val account: Account,
        override val resources: List<TransferableX>
    ) : AccountWithTransferableResources {
        override val address: AccountAddress
            get() = account.address
    }

    data class Other(
        override val address: AccountAddress,
        override val resources: List<TransferableX>
    ) : AccountWithTransferableResources

    fun updateFromGuarantees( // TODO needs update
        accountsWithPredictedGuarantees: List<AccountWithPredictedGuarantee>
    ): AccountWithTransferableResources {
        val resourcesWithGuaranteesForAccount = accountsWithPredictedGuarantees.filter {
            it.address == address
        }

        val resources = resources.mapWhen(
            predicate = { depositing -> // this is now Transferable
                resourcesWithGuaranteesForAccount.any {
                    it.address == address && it.transferable.resourceAddress == depositing.resourceAddress
                }
            },
            mutation = { depositing ->
//                val accountWithGuarantee = resourcesWithGuaranteesForAccount.find {
//                    it.transferable.resourceAddress == depositing.transferable.resourceAddress
//                }
//
//                if (accountWithGuarantee != null) {
//                    depositing.updateGuarantee(accountWithGuarantee.guaranteeOffsetDecimal)
//                } else {
                    depositing
//                }
            }
        )
        return when (this) {
            is Other -> copy(resources = resources)
            is Owned -> copy(resources = resources)
        }
    }

    companion object {
        class Sorter(
            private val ownedAccountsOrder: List<Account>
        ) : Comparator<AccountWithTransferableResources> {
            override fun compare(thisAccount: AccountWithTransferableResources?, otherAccount: AccountWithTransferableResources?): Int {
                val indexOfThisAccount = ownedAccountsOrder.indexOfFirst { it.address == thisAccount?.address }
                val indexOfOtherAccount = ownedAccountsOrder.indexOfFirst { it.address == otherAccount?.address }

                return if (indexOfThisAccount == -1 && indexOfOtherAccount >= 0) {
                    1 // The other account is owned, so it takes higher priority
                } else if (indexOfOtherAccount == -1 && indexOfThisAccount >= 0) {
                    -1 // This account is owned, so it takes higher priority
                } else if (indexOfThisAccount == -1 && indexOfOtherAccount == -1) {
                    0 // Both accounts are not owned, both considered equal, so they will be sorted according to the receiving order
                } else {
                    indexOfThisAccount - indexOfOtherAccount
                }
            }
        }
    }
}
