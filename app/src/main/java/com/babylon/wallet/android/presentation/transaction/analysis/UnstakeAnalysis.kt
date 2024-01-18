package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
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
): PreviewType.Transfer.Staking {
    val fromAccounts = extractWithdrawals(executionSummary, getProfileUseCase, resources, involvedValidators)
    val toAccounts = extractDeposits(executionSummary, getProfileUseCase, resources, involvedValidators)
    val validatorAddressesSet = validatorAddresses.map { it.addressString() }.toSet()
    return PreviewType.Transfer.Staking(
        from = fromAccounts,
        to = toAccounts,
        validators = involvedValidators.filter { it.address in validatorAddressesSet },
        actionType = PreviewType.Transfer.Staking.ActionType.Unstake
    )
}

private suspend fun DetailedManifestClass.ValidatorUnstake.extractDeposits(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
) = executionSummary.accountDeposits.map { claimsPerAddress ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
    val depositingNfts = claimsPerAddress.value.map { claimedResource ->
        val resourceAddress = claimedResource.resourceAddress
        val nftResource =
            resources.find { it.resourceAddress == resourceAddress } as? Resource.NonFungibleResource
                ?: error("No resource found")
        val validatorUnstake = validatorUnstakes.find { it.claimNftAddress.addressString() == resourceAddress }
            ?: error("No validator claim found")
        val lsuResource = resources.find {
            it.resourceAddress == validatorUnstake.liquidStakeUnitAddress.addressString()
        } as? Resource.FungibleResource ?: error("No resource found")
        val validator =
            involvedValidators.find { validatorUnstake.validatorAddress.addressString() == it.address } ?: error("No validator found")
        val lsuAmount = validatorUnstake.liquidStakeUnitAmount.asStr().toBigDecimal()
        val xrdWorth =
            lsuAmount.divide(lsuResource.currentSupply, lsuResource.mathContext).multiply(validator.totalXrdStake, lsuResource.mathContext)
        val stakeClaimNftItems = validatorUnstake.claimNftIds.map { localId ->
            Resource.NonFungibleResource.Item(
                collectionAddress = resourceAddress,
                localId = Resource.NonFungibleResource.Item.ID.from(localId)
            ) to xrdWorth
        }
        Transferable.Depositing(
            transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                nftResource.copy(items = stakeClaimNftItems.map { it.first }),
                validator,
                stakeClaimNftItems.associate {
                    it.first.localId.displayable to it.second
                }
            )
        )
    }
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = depositingNfts
    )
}

private suspend fun DetailedManifestClass.ValidatorUnstake.extractWithdrawals(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
) = executionSummary.accountWithdraws.map { withdrawalsPerAccount ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(withdrawalsPerAccount.key) ?: error("No account found")
    val withdrawingLsu = withdrawalsPerAccount.value.groupBy { it.resourceAddress }.map { depositedResource ->
        val resourceAddress = depositedResource.key
        val lsuResource = resources.find {
            it.resourceAddress == resourceAddress
        } as? Resource.FungibleResource ?: error("No resource found")
        val unstakes = validatorUnstakes.filter { it.liquidStakeUnitAddress.asStr() == resourceAddress }
        val validator =
            involvedValidators.find { it.address == unstakes.first().validatorAddress.addressString() } ?: error("No validator found")
        val totalLSU = unstakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() }
        val xrdWorth =
            totalLSU.divide(lsuResource.currentSupply, lsuResource.mathContext).multiply(validator.totalXrdStake, lsuResource.mathContext)
        Transferable.Withdrawing(
            transferable = TransferableAsset.Fungible.LSUAsset(
                totalLSU,
                LiquidStakeUnit(lsuResource),
                validator,
                xrdWorth,
            )
        )
    }
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = withdrawingLsu
    )
}
