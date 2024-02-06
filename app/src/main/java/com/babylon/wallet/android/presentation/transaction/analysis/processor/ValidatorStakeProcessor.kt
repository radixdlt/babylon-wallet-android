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
import kotlinx.coroutines.flow.first
import rdx.works.core.divideWithDivisibility
import rdx.works.core.multiplyWithDivisibility
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getValidatorsUseCase: GetValidatorsUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses + xrdAddress).getOrThrow()
        val involvedValidators = getValidatorsUseCase(classification.involvedValidatorAddresses).getOrThrow()

        val fromAccounts = extractWithdrawals(summary, getProfileUseCase, resources)
        val toAccounts = classification.extractDeposits(
            executionSummary = summary,
            getProfileUseCase = getProfileUseCase,
            resources = resources,
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
        resources: List<Resource>,
        involvedValidators: List<ValidatorDetail>
    ) = executionSummary.accountDeposits.map { depositsPerAccount ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAccount.key) ?: error("No account found")
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val depositingLsu = depositsPerAccount.value.groupBy { it.resourceAddress }.map { depositedResources ->
            val lsuResource = resources.find {
                it.resourceAddress == depositedResources.key
            } as? Resource.FungibleResource ?: error("No resource found")
            val relatedStakes = validatorStakes.filter { it.liquidStakeUnitAddress.addressString() == lsuResource.resourceAddress }
            val totalStakedLsuForAccount = relatedStakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() }
            val totalStakeXrdWorthForAccount = relatedStakes.sumOf { it.xrdAmount.asStr().toBigDecimal() }
            val validatorAddress = lsuResource.validatorAddress ?: error("No validator address found")
            val validator =
                involvedValidators.find { it.address == validatorAddress } ?: error("No validator found")
            val lsuAmount = depositedResources.value.sumOf { it.amount }
            val xrdWorth = lsuAmount.divideWithDivisibility(totalStakedLsuForAccount, lsuResource.divisibility)
                .multiplyWithDivisibility(totalStakeXrdWorthForAccount, lsuResource.divisibility)
            val guaranteeType = depositedResources.value.first().guaranteeType(defaultDepositGuarantees)
            Transferable.Depositing(
                transferable = TransferableAsset.Fungible.LSUAsset(
                    amount = lsuAmount,
                    lsu = LiquidStakeUnit(lsuResource.copy(ownedAmount = lsuAmount), validator),
                    xrdWorth = xrdWorth,
                ),
                guaranteeType = guaranteeType
            )
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = depositingLsu
        )
    }

    private suspend fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        resources: List<Resource>
    ) = executionSummary.accountWithdraws.map { entry ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(entry.key) ?: error("No account found")
        val xrdResource = resources.find {
            it.resourceAddress == XrdResource.address(NetworkId.from(ownedAccount.networkID))
        } as? Resource.FungibleResource ?: error("No resource found")
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
