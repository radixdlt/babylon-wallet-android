package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.TransactionType
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

suspend fun TransactionType.StakeTransaction.resolve(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    validators: List<ValidatorDetail>
): PreviewType {
    val fromAccounts = mutableListOf<AccountWithTransferableResources>()
    val toAccounts = mutableListOf<AccountWithTransferableResources>()
    var finalValidators = listOf<ValidatorDetail>()
    stakes.groupBy { it.fromAccount.addressString() }.forEach { stakesPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(stakesPerAddress.key) ?: error("No account found")
        val xrdResource = resources.find {
            it.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
        } as? Resource.FungibleResource ?: error("No resource found")
        fromAccounts.add(
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = listOf(
                    element = Transferable.Withdrawing(
                        transferable = TransferableResource.Amount(
                            stakesPerAddress.value.sumOf { it.stakedXrd.asStr().toBigDecimal() },
                            xrdResource,
                            false
                        )
                    )
                )
            )
        )
        val depositingLsu = stakesPerAddress.value.groupBy {
            it.validatorAddress.addressString()
        }.map { stakesPerValidator ->
            val lsuResource =
                resources.find {
                    it.resourceAddress == stakesPerValidator.value.first().stakeUnitResource.addressString()
                } as? Resource.FungibleResource ?: error("No resource found")
            val validator = validators.find { it.address == stakesPerValidator.key } ?: error("No validator found")
            finalValidators = (finalValidators + validator).distinctBy { it.address }
            Transferable.Depositing(
                transferable = TransferableResource.LsuAmount(
                    stakesPerValidator.value.sumOf { it.stakeUnitAmount.asStr().toBigDecimal() },
                    lsuResource,
                    validator,
                    stakesPerValidator.value.sumOf { it.stakedXrd.asStr().toBigDecimal() },
                )
            )
        }
        toAccounts.add(
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = depositingLsu
            )
        )
    }
    return PreviewType.Stake(from = fromAccounts, to = toAccounts, validators = finalValidators)
}
