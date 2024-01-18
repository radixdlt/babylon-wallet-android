package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNFTDetailsUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.nonFungibleLocalIdAsStr
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorClaimProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase,
    private val getNFTDetailsUseCase: GetNFTDetailsUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorClaim> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorClaim): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses + xrdAddress).getOrThrow()
        val involvedValidators = getValidatorsUseCase(classification.involvedValidatorAddresses).getOrThrow()
        val stakeClaimsNfts = summary.involvedStakeClaims.map {
            getNFTDetailsUseCase(it.resourceAddress, it.localId).getOrDefault(emptyList())
        }.flatten()

        val toAccounts = extractDeposits(summary, getProfileUseCase, resources)
        val fromAccounts = classification.extractWithdrawals(
            executionSummary = summary,
            getProfileUseCase = getProfileUseCase,
            resources = resources,
            involvedValidators = involvedValidators,
            stakeClaimsNfts = stakeClaimsNfts
        )
        return PreviewType.Transfer.Staking(
            validators = involvedValidators,
            from = fromAccounts,
            to = toAccounts,
            actionType = PreviewType.Transfer.Staking.ActionType.ClaimStake
        )
    }

    private suspend fun DetailedManifestClass.ValidatorClaim.extractWithdrawals(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        resources: List<Resource>,
        involvedValidators: List<ValidatorDetail>,
        stakeClaimsNfts: List<Resource.NonFungibleResource.Item>
    ): List<AccountWithTransferableResources.Owned> {
        return executionSummary.accountWithdraws.map { claimsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
            val withdrawingNfts = claimsPerAddress.value.distinctBy { it.resourceAddress }.map { resourceClaim ->
                val resourceAddress = resourceClaim.resourceAddress
                val nftResource =
                    resources.find { it.resourceAddress == resourceAddress } as? Resource.NonFungibleResource
                        ?: error("No resource found")
                val validatorClaim = validatorClaims.find { it.claimNftAddress.addressString() == resourceAddress }
                    ?: error("No validator claim found")
                val validator =
                    involvedValidators.find { validatorClaim.validatorAddress.addressString() == it.address } ?: error("No validator found")
                val items = validatorClaims.filter { it.claimNftAddress.addressString() == resourceAddress }.map { claim ->
                    claim.claimNftIds.map { localId ->
                        val localIdString = nonFungibleLocalIdAsStr(localId)
                        val claimAmount = stakeClaimsNfts.find {
                            resourceAddress == it.collectionAddress && localIdString == nonFungibleLocalIdAsStr(it.localId.toRetId())
                        }?.claimAmountXrd
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceAddress,
                            localId = Resource.NonFungibleResource.Item.ID.from(localId)
                        ) to (claimAmount ?: claim.xrdAmount.asStr().toBigDecimal())
                    }
                }.flatten()
                Transferable.Withdrawing(
                    transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                        nftResource.copy(items = items.map { it.first }),
                        validator,
                        items.associate {
                            it.first.localId.displayable to it.second
                        }
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
                    transferable = TransferableAsset.Fungible.Token(
                        entry.value.sumOf { it.amount },
                        xrdResource,
                        false
                    )
                )
            )
        )
    }
}
