package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.times
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.roundedWith
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val networkId = getProfileUseCase().currentGateway.network.id
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
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
        involvedOwnedAccounts: List<Account>
    ) = executionSummary.deposits.map { depositsPerAccount ->
        val defaultDepositGuarantees = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
        depositsPerAccount.value.map { depositedResource ->
            val asset = assets.find {
                it.resource.address == depositedResource.address
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
        defaultDepositGuarantees: Decimal192
    ): Transferable.Depositing {
        val relatedStakes = validatorStakes.filter { it.liquidStakeUnitAddress == asset.resourceAddress }
        val totalStakedLsuForAccount = relatedStakes.sumOf { it.liquidStakeUnitAmount }
        val totalStakeXrdWorthForAccount = relatedStakes.sumOf { it.xrdAmount }
        val lsuAmount = depositedResource.amount
        val xrdWorth = ((lsuAmount / totalStakedLsuForAccount) * totalStakeXrdWorthForAccount).roundedWith(asset.resource.divisibility)
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
