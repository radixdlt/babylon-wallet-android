package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.core.divideWithDivisibility
import rdx.works.core.multiplyWithDivisibility
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId.value)
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses() + xrdAddress,
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val fromAccounts = summary.toWithdrawingAccountsWithTransferableAssets(
            involvedAssets = assets,
            allOwnedAccounts = involvedOwnedAccounts
        )
        val toAccounts = classification.extractDeposits(
            executionSummary = summary,
            assets = assets,
            involvedOwnedAccounts = involvedOwnedAccounts
        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))
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
        involvedOwnedAccounts: List<Network.Account>
    ) = executionSummary.accountDeposits.map { depositsPerAccount ->
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        depositsPerAccount.value.map { depositedResource ->
            val asset = assets.find {
                it.resource.resourceAddress == depositedResource.resourceAddress
            } ?: error("No asset found")
            if (asset is LiquidStakeUnit) {
                resolveLSU(asset, depositedResource, defaultDepositGuarantees)
            } else {
                executionSummary.resolveDepositingAsset(depositedResource, assets, defaultDepositGuarantees)
            }
        }.toAccountWithTransferableResources(depositsPerAccount.key, involvedOwnedAccounts)
    }

    private fun DetailedManifestClass.ValidatorStake.resolveLSU(
        asset: LiquidStakeUnit,
        depositedResource: ResourceIndicator,
        defaultDepositGuarantees: Double
    ): Transferable.Depositing {
        val relatedStakes = validatorStakes.filter { it.liquidStakeUnitAddress.addressString() == asset.resourceAddress }
        val totalStakedLsuForAccount = relatedStakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() }
        val totalStakeXrdWorthForAccount = relatedStakes.sumOf { it.xrdAmount.asStr().toBigDecimal() }
        val lsuAmount = depositedResource.amount
        val xrdWorth = lsuAmount.divideWithDivisibility(totalStakedLsuForAccount, asset.resource.divisibility)
            .multiplyWithDivisibility(totalStakeXrdWorthForAccount, asset.resource.divisibility)
        val guaranteeType = depositedResource.guaranteeType(defaultDepositGuarantees)
        return Transferable.Depositing(
            transferable = TransferableAsset.Fungible.LSUAsset(
                amount = lsuAmount,
                lsu = LiquidStakeUnit(asset.resource.copy(ownedAmount = lsuAmount), asset.validator),
                xrdWorth = xrdWorth,
            ),
            guaranteeType = guaranteeType
        )
    }
}
