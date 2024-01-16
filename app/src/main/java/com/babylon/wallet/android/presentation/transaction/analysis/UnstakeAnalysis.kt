package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

suspend fun DetailedManifestClass.ValidatorUnstake.resolve(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
): PreviewType {
    val fromAccounts = extractWithdrawals(executionSummary, getProfileUseCase, resources, involvedValidators)
    val toAccounts = extractDeposits(executionSummary, getProfileUseCase, resources, involvedValidators)
    val validatorAddressesSet = validatorAddresses.map { it.addressString() }.toSet()
    return PreviewType.Staking(
        from = fromAccounts,
        to = toAccounts,
        validators = involvedValidators.filter { it.address in validatorAddressesSet },
        actionType = PreviewType.Staking.ActionType.Unstake
    )
}

private suspend fun DetailedManifestClass.ValidatorUnstake.extractDeposits(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
) = executionSummary.accountDeposits.map { claimsPerAddress ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
    val withdrawingNfts = claimsPerAddress.value.map { claimedResource ->
        val resourceAddress = claimedResource.resourceAddress
        val nftResource =
            resources.find { it.resourceAddress == resourceAddress } as? Resource.NonFungibleResource
                ?: error("No resource found")
        val validatorUnstake = validatorUnstakes.find { it.claimNftAddress.addressString() == resourceAddress }
            ?: error("No validator claim found")
        val validator =
            involvedValidators.find { validatorUnstake.validatorAddress.addressString() == it.address } ?: error("No validator found")
        val stakeClaimNftItems = validatorUnstake.claimNftIds.map { localId ->
            // TODO for a claim that has multiple claimNftLocalIds, we don't have a way now to determine its XRD worth
            // this will need to be changed once RET provide xrd worth of each claim
            Resource.NonFungibleResource.Item(
                collectionAddress = resourceAddress,
                localId = Resource.NonFungibleResource.Item.ID.from(localId)
            ) to validatorUnstake.liquidStakeUnitAmount.asStr().toBigDecimal()
        }
        Transferable.Depositing(
            transferable = TransferableResource.StakeClaimNft(
                nftResource.copy(items = stakeClaimNftItems.map { it.first }),
                stakeClaimNftItems.associate {
                    it.first.localId.displayable to it.second
                },
                validator,
                true
            )
        )
    }
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = withdrawingNfts
    )
}

private suspend fun DetailedManifestClass.ValidatorUnstake.extractWithdrawals(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
) = executionSummary.accountWithdraws.map { withdrawalsPerAccount ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(withdrawalsPerAccount.key) ?: error("No account found")
    val withdrawingLsu = withdrawalsPerAccount.value.map { depositedResource ->
        val resourceAddress = depositedResource.resourceAddress
        val lsuResource = resources.find {
            it.resourceAddress == resourceAddress
        } as? Resource.FungibleResource ?: error("No resource found")
        val unstakes = validatorUnstakes.filter { it.liquidStakeUnitAddress.asStr() == resourceAddress }
        val validator =
            involvedValidators.find { it.address == unstakes.first().validatorAddress.addressString() } ?: error("No validator found")
        Transferable.Withdrawing(
            transferable = TransferableResource.LsuAmount(
                unstakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() },
                lsuResource,
                validator,
                unstakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() },
            )
        )
    }
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = withdrawingLsu
    )
}
