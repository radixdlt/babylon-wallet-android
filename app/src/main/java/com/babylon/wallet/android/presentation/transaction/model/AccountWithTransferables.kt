package com.babylon.wallet.android.presentation.transaction.model

import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Profile
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork

data class AccountWithTransferables(
    val account: InvolvedAccount,
    val transferables: List<Transferable>,
    val additionalTransferablesPresent: Boolean = false
) {

    fun hashCustomisableGuarantees() = transferables.any { it.amount is FungibleAmount.Predicted }

    fun updateFromGuarantees(
        guaranteeItems: List<GuaranteeItem>
    ): AccountWithTransferables {
        val guaranteesRelatedToAccount = guaranteeItems.filter { it.account.address == account.address }.associate {
            it.transferable.resourceAddress to it.updatedAmount
        }
        val updatedTransferables = transferables.map { transferable ->
            val updatedAmount = guaranteesRelatedToAccount[transferable.resourceAddress] ?: return@map transferable

            when (transferable) {
                is Transferable.FungibleType.LSU -> transferable.copy(amount = updatedAmount)
                is Transferable.FungibleType.PoolUnit -> transferable.copy(amount = updatedAmount)
                is Transferable.FungibleType.Token -> transferable.copy(amount = updatedAmount)
                else -> transferable
            }
        }

        return update(updatedTransferables)
    }

    fun update(transferables: List<Transferable>): AccountWithTransferables = copy(transferables = transferables)

    companion object {
        class Sorter(
            private val ownedAccountsOrder: List<Account>
        ) : Comparator<AccountWithTransferables> {

            constructor(profile: Profile) : this(profile.activeAccountsOnCurrentNetwork)

            override fun compare(thisAccount: AccountWithTransferables?, otherAccount: AccountWithTransferables?): Int {
                val indexOfThisAccount = ownedAccountsOrder.indexOfFirst { it.address == thisAccount?.account?.address }
                val indexOfOtherAccount = ownedAccountsOrder.indexOfFirst { it.address == otherAccount?.account?.address }

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
