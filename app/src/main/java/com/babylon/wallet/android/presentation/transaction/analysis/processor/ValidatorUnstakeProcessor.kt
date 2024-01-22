package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
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

        val fromAccounts = classification.extractWithdrawals(summary, getProfileUseCase, resources, involvedValidators)
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
            val xrdWorth = lsuAmount.divide(lsuResource.currentSupply, lsuResource.mathContext)
                .multiply(validator.totalXrdStake, lsuResource.mathContext)
            val stakeClaimNftItems = validatorUnstake.claimNftIds.map { localId ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(localId)
                ) to xrdWorth
            }
            Transferable.Depositing(
                transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                    resource = nftResource.copy(items = stakeClaimNftItems.map { it.first }),
                    validator = validator,
                    xrdWorthPerNftItem = stakeClaimNftItems.associate {
                        it.first.localId.displayable to it.second
                    },
                    isNewlyCreated = true
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
        val withdrawingLsu = withdrawalsPerAccount.value.map { depositedResource ->
            val resourceAddress = depositedResource.resourceAddress
            val lsuResource = resources.find {
                it.resourceAddress == resourceAddress
            } as? Resource.FungibleResource ?: error("No resource found")
            val unstakes = validatorUnstakes.filter { it.liquidStakeUnitAddress.asStr() == resourceAddress }
            val validator =
                involvedValidators.find { it.address == unstakes.first().validatorAddress.addressString() } ?: error("No validator found")
            val totalLSU = unstakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() }
            val xrdWorth = totalLSU.divide(lsuResource.currentSupply, lsuResource.mathContext)
                .multiply(validator.totalXrdStake, lsuResource.mathContext)
            Transferable.Withdrawing(
                transferable = TransferableAsset.Fungible.LSUAsset(
                    amount = totalLSU,
                    lsu = LiquidStakeUnit(lsuResource, validator),
                    validator = validator,
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
