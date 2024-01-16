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

suspend fun DetailedManifestClass.ValidatorStake.resolve(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
): PreviewType {
    val fromAccounts = extractWithdrawals(executionSummary, getProfileUseCase, resources)
    val toAccounts = extractDeposits(
        executionSummary = executionSummary,
        getProfileUseCase = getProfileUseCase,
        resources = resources,
        involvedValidators = involvedValidators
    )
    return PreviewType.Staking(
        from = fromAccounts,
        to = toAccounts,
        validators = involvedValidators,
        actionType = PreviewType.Staking.ActionType.Stake
    )
}

private suspend fun DetailedManifestClass.ValidatorStake.extractDeposits(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedValidators: List<ValidatorDetail>
) = executionSummary.accountDeposits.map { depositsPerAccount ->
    val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAccount.key) ?: error("No account found")
    val depositingLsu = depositsPerAccount.value.map { depositedResource ->
        val resourceAddress = depositedResource.resourceAddress
        val lsuResource = resources.find {
            it.resourceAddress == resourceAddress
        } as? Resource.FungibleResource ?: error("No resource found")
        val stakes = validatorStakes.filter { it.liquidStakeUnitAddress.asStr() == resourceAddress }
        val validator =
            involvedValidators.find { it.address == stakes.first().validatorAddress.addressString() } ?: error("No validator found")
        Transferable.Depositing(
            transferable = TransferableResource.LsuAmount(
                stakes.sumOf { it.liquidStakeUnitAmount.asStr().toBigDecimal() },
                lsuResource,
                validator,
                stakes.sumOf { it.xrdAmount.asStr().toBigDecimal() },
            )
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
    AccountWithTransferableResources.Owned(
        account = ownedAccount,
        resources = listOf(
            element = Transferable.Withdrawing(
                transferable = TransferableResource.Amount(
                    entry.value.sumOf { it.amount },
                    xrdResource,
                    false
                )
            )
        )
    )
}
