package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork

suspend fun DetailedManifestClass.ValidatorClaim.resolve(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
): PreviewType {
    val toAccounts = extractDeposits(executionSummary, getProfileUseCase, resources)
    val fromAccounts = extractWithdrawals(
        executionSummary = executionSummary,
        getProfileUseCase = getProfileUseCase,
        resources = resources,
        involvedValidators = involvedValidators
    )
    return PreviewType.Staking(
        validators = involvedValidators,
        from = fromAccounts,
        to = toAccounts,
        actionType = PreviewType.Staking.ActionType.ClaimStake
    )
}

private suspend fun DetailedManifestClass.ValidatorClaim.extractWithdrawals(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
): List<AccountWithTransferableResources.Owned> {
    return executionSummary.accountWithdraws.map { claimsPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
        val withdrawingNfts = claimsPerAddress.value.map { claimedResource ->
            val resourceAddress = claimedResource.resourceAddress
            val nftResource =
                resources.find { it.resourceAddress == resourceAddress } as? Resource.NonFungibleResource
                    ?: error("No resource found")
            val validatorClaim = validatorClaims.find { it.claimNftAddress.addressString() == resourceAddress }
                ?: error("No validator claim found")
            val validator =
                involvedValidators.find { validatorClaim.validatorAddress.addressString() == it.address } ?: error("No validator found")
            val stakeClaimNftItems = validatorClaim.claimNftIds.map { localId ->
                // TODO for a claim that has multiple claimNftLocalIds, we don't have a way now to determine its XRD worth
                // this will need to be changed once RET provide xrd worth of each claim
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(localId)
                ) to validatorClaim.xrdAmount.asStr().toBigDecimal()
            }
            Transferable.Withdrawing(
                transferable = TransferableResource.StakeClaimNft(
                    nftResource.copy(items = stakeClaimNftItems.map { it.first }),
                    stakeClaimNftItems.associate {
                        it.first.localId.displayable to it.second
                    },
                    validator,
                    false
                )
            )
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = withdrawingNfts
        )
    }
}

private suspend fun extractDeposits(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>
) = executionSummary.accountDeposits.map { entry ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: error("No account found")
    val xrdResource = resources.find {
        it.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
    } as? Resource.FungibleResource ?: error("No resource found")
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = listOf(
            element = Transferable.Depositing(
                transferable = TransferableResource.Amount(
                    entry.value.sumOf { it.amount },
                    xrdResource,
                    false
                )
            )
        )
    )
}
