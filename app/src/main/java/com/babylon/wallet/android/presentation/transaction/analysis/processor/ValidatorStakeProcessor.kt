package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses() + xrdAddress,
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }
        val allOwnedAccounts = summary.allOwnedAccounts(getProfileUseCase)
        val fromAccounts = summary.toWithdrawingAccountsWithTransferableAssets(
            involvedAssets = assets,
            allOwnedAccounts = allOwnedAccounts
        )
        val toAccounts = classification.extractDeposits(
            executionSummary = summary,
            assets = assets,
            involvedValidators = involvedValidators
        )
        return PreviewType.Transfer.Staking(
            from = fromAccounts,
            to = toAccounts,
            validators = involvedValidators,
            actionType = PreviewType.Transfer.Staking.ActionType.Stake
        )
    }

    private suspend fun DetailedManifestClass.ValidatorStake.extractDeposits(
        executionSummary: ExecutionSummary,
        assets: List<Asset>,
        involvedValidators: List<ValidatorDetail>
    ) = executionSummary.accountDeposits.map { depositsPerAccount ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAccount.key) ?: error("No account found")
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val depositingTransferable = depositsPerAccount.value.map { depositedResource ->
            val resource = assets.find {
                it.resource.resourceAddress == depositedResource.resourceAddress
            }?.resource ?: error("No resource found")
            val validatorAddress = resource.validatorAddress
            if (validatorAddress == null) {
                executionSummary.resolveDepositingAsset(depositedResource, assets, defaultDepositGuarantees)
            } else {
                resolveLSU(resource, involvedValidators, validatorAddress, depositedResource, defaultDepositGuarantees)
            }
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = depositingTransferable
        )
    }

    private fun DetailedManifestClass.ValidatorStake.resolveLSU(
        resource: Resource,
        involvedValidators: List<ValidatorDetail>,
        validatorAddress: String?,
        depositedResource: ResourceIndicator,
        defaultDepositGuarantees: Double
    ): Transferable.Depositing {
        resource as? Resource.FungibleResource ?: error("No fungible resource found")
        val relatedStakes = validatorStakes.filter { it.liquidStakeUnitAddress.addressString() == resource.resourceAddress }
        val totalStakedLsuForAccount = relatedStakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() }
        val totalStakeXrdWorthForAccount = relatedStakes.sumOf { it.xrdAmount.asStr().toBigDecimal() }
        val validator =
            involvedValidators.find { it.address == validatorAddress } ?: error("No validator found")
        val lsuAmount = depositedResource.amount
        val xrdWorth = lsuAmount.divide(totalStakedLsuForAccount, resource.mathContext)
            .multiply(totalStakeXrdWorthForAccount, resource.mathContext)
        val guaranteeType = depositedResource.guaranteeType(defaultDepositGuarantees)
        return Transferable.Depositing(
            transferable = TransferableAsset.Fungible.LSUAsset(
                amount = lsuAmount,
                lsu = LiquidStakeUnit(resource.copy(ownedAmount = lsuAmount), validator),
                xrdWorth = xrdWorth,
            ),
            guaranteeType = guaranteeType
        )
    }
}
