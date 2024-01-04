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
    var finalValidators = listOf<ValidatorDetail>()
    val totalStakedXrdPerAccount = stakes.map { stake ->
        stake.fromAccount.addressString() to stake.stakedXrd.asStr().toBigDecimal()
    }.groupBy { it.first }.mapValues { addressToStake -> addressToStake.value.sumOf { it.second } }
    val transferableListPerAddress = mutableMapOf<String, List<Transferable>>()
    stakes.forEach { stakeInformation ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(stakeInformation.fromAccount.addressString())
            ?: return@forEach
        val xrdResource = resources.find {
            it.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
        } as? Resource.FungibleResource ?: return@forEach
        val lsuResource =
            resources.find { it.resourceAddress == stakeInformation.stakeUnitResource.addressString() } as? Resource.FungibleResource
                ?: return@forEach
        val stakeXrdValue = totalStakedXrdPerAccount[ownedAccount.address] ?: return@forEach
        if (fromAccounts.none { it.address == ownedAccount.address }) {
            fromAccounts.add(
                AccountWithTransferableResources.Owned(
                    account = ownedAccount,
                    resources = listOf(
                        element = Transferable.Withdrawing(
                            transferable = TransferableResource.Amount(
                                stakeXrdValue,
                                xrdResource,
                                false
                            )
                        )
                    )
                )
            )
        }
        val validator = validators.find { it.address == stakeInformation.validatorAddress.addressString() } ?: return@forEach
        finalValidators = (finalValidators + validator).distinctBy { it.address }
        val depositingLsu = Transferable.Depositing(
            transferable = TransferableResource.LsuAmount(
                stakeInformation.stakeUnitAmount.asStr().toBigDecimal(),
                lsuResource,
                validator,
                stakeInformation.stakedXrd.asStr().toBigDecimal()
            )
        )
        if (transferableListPerAddress.containsKey(ownedAccount.address)) {
            transferableListPerAddress[ownedAccount.address] = transferableListPerAddress[ownedAccount.address]!! + depositingLsu
        } else {
            transferableListPerAddress[ownedAccount.address] = listOf(depositingLsu)
        }
    }
    val toAccounts = transferableListPerAddress.map { entry ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: return@map null
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = entry.value
        )
    }.filterNotNull()

    return PreviewType.Stake(from = fromAccounts, to = toAccounts, validators = finalValidators)
}
