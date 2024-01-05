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

@Suppress("LongMethod")
suspend fun TransactionType.ClaimStakeTransaction.resolve(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    validators: List<ValidatorDetail>
): PreviewType {
    val fromAccounts = mutableListOf<AccountWithTransferableResources>()
    val toAccounts = mutableListOf<AccountWithTransferableResources>()
    var finalValidators = listOf<ValidatorDetail>()
    claims.groupBy { it.fromAccount.addressString() }.forEach { claimsPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
        val xrdResource = resources.find {
            it.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
        } as? Resource.FungibleResource ?: error("No resource found")
        toAccounts.add(
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = listOf(
                    element = Transferable.Withdrawing(
                        transferable = TransferableResource.Amount(
                            claimsPerAddress.value.sumOf { it.claimedXrd.asStr().toBigDecimal() },
                            xrdResource,
                            false
                        )
                    )
                )
            )
        )
        claimsPerAddress.value.forEach { it.claimedXrd }

        claimsPerAddress.value.groupBy { it.validatorAddress.addressString() }.map { claimsPerValidator ->
            val validator = validators.find { it.address == claimsPerValidator.key } ?: error("No validator found")
            finalValidators = (finalValidators + validator).distinctBy { it.address }
            val withdrawingNfts = claimsPerValidator.value.groupBy { it.claimNftResource.addressString() }.map { claimsPerResource ->
                // here we group claims per ntf resource address, because depending on how manifest is constructed, sometimes multiple
                // NFT token claims are grouped under one "claim" object,
                // and sometimes there are multiple claim objects, each with one NFT token claim
                val nftResource =
                    resources.find { it.resourceAddress == claimsPerResource.key } as? Resource.NonFungibleResource
                        ?: error("No resource found")
                val stakeClaimNftItems = claimsPerResource.value.map { claim ->
                    claim.claimNftLocalIds.map {
                        // for a claim that has multiple claimNftLocalIds, we don't have a way now to determine its XRD worth
                        // this will need to be changed once RET provide xrd worth of each claim
                        Resource.NonFungibleResource.Item(
                            collectionAddress = claim.claimNftResource.addressString(),
                            localId = Resource.NonFungibleResource.Item.ID.from(claim.claimNftLocalIds.first())
                        ) to claim.claimedXrd.asStr().toBigDecimal()
                    }
                }.flatten().distinctBy { it.first.localId.displayable }
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
            fromAccounts.add(
                AccountWithTransferableResources.Owned(
                    account = ownedAccount,
                    resources = withdrawingNfts
                )
            )
        }
    }
    return PreviewType.Staking(
        validators = finalValidators,
        from = fromAccounts,
        to = toAccounts,
        actionType = PreviewType.Staking.ActionType.ClaimStake
    )
}
