package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.GetValidatorsUseCase
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import kotlinx.coroutines.flow.first
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses() + xrdAddress,
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val involvedValidators = getValidatorsUseCase(classification.involvedValidatorAddresses).getOrThrow()

        val fromAccounts = extractWithdrawals(summary, getProfileUseCase, assets)
        val toAccounts = classification.extractDeposits(
            executionSummary = summary,
            getProfileUseCase = getProfileUseCase,
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
        getProfileUseCase: GetProfileUseCase,
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
                executionSummary.resolveGeneralAsset(depositedResource, assets, defaultDepositGuarantees)
            } else {
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
                Transferable.Depositing(
                    transferable = TransferableAsset.Fungible.LSUAsset(
                        amount = lsuAmount,
                        lsu = LiquidStakeUnit(resource.copy(ownedAmount = lsuAmount), validator),
                        xrdWorth = xrdWorth,
                    ),
                    guaranteeType = guaranteeType
                )
            }
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = depositingTransferable
        )
    }

    private suspend fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        resources: List<Asset>
    ) = executionSummary.accountWithdraws.map { entry ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: error("No account found")
        val xrdResource = resources.find {
            it.resource.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
        }?.resource as? Resource.FungibleResource ?: error("No resource found")
        val amount = entry.value.sumOf { it.amount }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = listOf(
                element = Transferable.Withdrawing(
                    transferable = TransferableAsset.Fungible.Token(
                        amount = amount,
                        resource = xrdResource.copy(ownedAmount = amount),
                        isNewlyCreated = false
                    )
                )
            )
        )
    }
}
