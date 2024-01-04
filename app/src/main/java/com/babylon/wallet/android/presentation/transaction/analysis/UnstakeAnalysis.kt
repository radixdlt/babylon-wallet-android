package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.TransactionType
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

@Suppress("UnsafeCallOnNullableType", "LongMethod")
suspend fun TransactionType.UnstakeTransaction.resolve(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    validators: List<ValidatorDetail>
): PreviewType {
    var finalValidators = listOf<ValidatorDetail>()
    val withdrawingTransferableListPerAddress = mutableMapOf<String, List<Transferable>>()
    val depositingTransferableListPerAddress = mutableMapOf<String, List<Transferable>>()
    unstakes.forEach { unstakeInformation ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(unstakeInformation.fromAccount.addressString())
            ?: return@forEach
        val lsuResource =
            resources.find { it.resourceAddress == unstakeInformation.stakeUnitAddress.addressString() } as? Resource.FungibleResource
                ?: return@forEach
        val nftGlobalId =
            NonFungibleGlobalId.fromParts(unstakeInformation.claimNftResource, unstakeInformation.claimNftLocalId).resourceAddress()
                .addressString()
        val nftResource =
            resources.find { it.resourceAddress == nftGlobalId } as? Resource.NonFungibleResource
                ?: return@forEach
        val validator = validators.find {
            it.address == unstakeInformation.validatorAddress.addressString()
        } ?: return@forEach
        finalValidators = (finalValidators + validator).distinctBy { it.address }
        val lsuAmount = unstakeInformation.stakeUnitAmount.asStr().toBigDecimal()
        val withdrawingLsu = Transferable.Withdrawing(
            transferable = TransferableResource.LsuAmount(
                lsuAmount,
                lsuResource,
                validator,
                unstakeInformation.claimNftData.claimAmount.asStr().toBigDecimal()
            )
        )
        if (withdrawingTransferableListPerAddress.containsKey(ownedAccount.address)) {
            withdrawingTransferableListPerAddress[ownedAccount.address] =
                withdrawingTransferableListPerAddress[ownedAccount.address]!! + withdrawingLsu
        } else {
            withdrawingTransferableListPerAddress[ownedAccount.address] = listOf(withdrawingLsu)
        }
        val depositingNft = Transferable.Depositing(
            transferable = TransferableResource.StakeClaimNft(
                nftResource,
                unstakeInformation.claimNftData.claimAmount.asStr().toBigDecimal(),
                validator,
                false
            )
        )
        if (depositingTransferableListPerAddress.containsKey(ownedAccount.address)) {
            depositingTransferableListPerAddress[ownedAccount.address] =
                depositingTransferableListPerAddress[ownedAccount.address]!! + depositingNft
        } else {
            depositingTransferableListPerAddress[ownedAccount.address] = listOf(depositingNft)
        }
    }
    val fromAccounts = withdrawingTransferableListPerAddress.map { entry ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: return@map null
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = entry.value
        )
    }.filterNotNull()
    val toAccounts = depositingTransferableListPerAddress.map { entry ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: return@map null
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = entry.value
        )
    }.filterNotNull()

    return PreviewType.Unstake(from = fromAccounts, to = toAccounts, validators = finalValidators)
}
