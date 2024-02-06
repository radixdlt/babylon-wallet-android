package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.core.divideWithDivisibility
import rdx.works.core.multiplyWithDivisibility
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorUnstakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorUnstake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorUnstake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses + xrdAddress, withDetails = true).getOrThrow()
        val involvedValidators = getValidatorsUseCase(classification.involvedValidatorAddresses).getOrThrow()

        val fromAccounts = extractWithdrawals(summary, getProfileUseCase, resources, involvedValidators)
        val toAccounts = classification.extractDeposits(summary, getProfileUseCase, resources, involvedValidators)
        val validatorAddressesSet = classification.validatorAddresses.map { it.addressString() }.toSet()
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
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val depositingNfts = claimsPerAddress.value.filterIsInstance<ResourceIndicator.NonFungible>().map { claimedResource ->
            val resourceAddress = claimedResource.resourceAddress.addressString()
            val nftResource =
                resources.find { it.resourceAddress == resourceAddress } as? Resource.NonFungibleResource
                    ?: error("No resource found")
            val validator =
                involvedValidators.find { nftResource.validatorAddress == it.address } ?: error("No validator found")
            val stakeClaimNftItems = claimedResource.indicator.nonFungibleLocalIds.map { localId ->
                val globalId = NonFungibleGlobalId.fromParts(claimedResource.resourceAddress, localId)
                val claimAmount =
                    claimsNonFungibleData.find { it.nonFungibleGlobalId.asStr() == globalId.asStr() }?.data?.claimAmount?.asStr()
                        ?.toBigDecimal()
                        ?: error("No claim amount found")
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(localId)
                ) to claimAmount
            }
            val guaranteeType = claimedResource.guaranteeType(defaultDepositGuarantees)
            Transferable.Depositing(
                transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                    claim = StakeClaim(
                        nonFungibleResource = nftResource.copy(items = stakeClaimNftItems.map { it.first }),
                        validator = validator
                    ),
                    xrdWorthPerNftItem = stakeClaimNftItems.associate {
                        it.first.localId.displayable to it.second
                    },
                    isNewlyCreated = true
                ),
                guaranteeType = guaranteeType
            )
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = depositingNfts
        )
    }

    private suspend fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        resources: List<Resource>,
        involvedValidators: List<ValidatorDetail>
    ) = executionSummary.accountWithdraws.map { withdrawalsPerAccount ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(withdrawalsPerAccount.key) ?: error("No account found")
        val withdrawingLsu = withdrawalsPerAccount.value.groupBy { it.resourceAddress }.map { depositedResources ->
            val resourceAddress = depositedResources.key
            val lsuResource = resources.find {
                it.resourceAddress == resourceAddress
            } as? Resource.FungibleResource ?: error("No resource found")
            val validatorAddress = lsuResource.validatorAddress ?: error("No validator address found")
            val validator = involvedValidators.find { it.address == validatorAddress } ?: error("No validator found")
            val totalLSU = depositedResources.value.sumOf { it.amount }
            val xrdWorth = totalLSU.divideWithDivisibility(lsuResource.currentSupply, lsuResource.divisibility)
                .multiplyWithDivisibility(validator.totalXrdStake, lsuResource.divisibility)
            Transferable.Withdrawing(
                transferable = TransferableAsset.Fungible.LSUAsset(
                    amount = totalLSU,
                    lsu = LiquidStakeUnit(lsuResource.copy(ownedAmount = totalLSU), validator),
                    xrdWorth = xrdWorth,
                )
            )
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = withdrawingLsu
        )
    }
}
