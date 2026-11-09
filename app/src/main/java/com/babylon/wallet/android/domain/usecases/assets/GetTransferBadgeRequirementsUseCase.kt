package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.TransferWithdrawProofs
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

/**
 * The badges a transfer of restricted tokens needs. Sargon reads the withdraw rules of the
 * transferred resources and what the account holds ([TransferWithdrawProofs]); this only adds
 * the badges' names and icons for the person.
 */
class GetTransferBadgeRequirementsUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val stateRepository: StateRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    data class Result(
        val status: BadgeRequirementStatus,
        /** The proofs to give to the transfer manifest. */
        val proofs: List<ResourceSpecifier>
    )

    suspend operator fun invoke(
        fromAccount: AccountAddress,
        resources: Set<ResourceAddress>
    ): kotlin.Result<Result> = sargonOsManager.callSafely(dispatcher) {
        transferWithdrawProofs(fromAccount = fromAccount, resources = resources.toList())
    }.map { outcome ->
        Result(status = outcome.toStatus(fromAccount), proofs = outcome.proofs)
    }

    private suspend fun TransferWithdrawProofs.toStatus(fromAccount: AccountAddress): BadgeRequirementStatus {
        if (restrictedWithdrawals.isEmpty()) return BadgeRequirementStatus.None

        val unsatisfied = restrictedWithdrawals.firstOrNull { !it.isSatisfied }
        return when {
            unsatisfied == null -> BadgeRequirementStatus.Success(
                resource = proofs.firstOrNull()?.let { resourceForDisplay(it.address, fromAccount) }
            )

            unsatisfied.badges.isEmpty() -> BadgeRequirementStatus.Error(
                resource = resourceForDisplay(unsatisfied.resource, fromAccount),
                reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable
            )

            else -> BadgeRequirementStatus.Error(
                resource = resourceForDisplay(unsatisfied.badges.first(), fromAccount),
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge
            )
        }
    }

    private suspend fun resourceForDisplay(address: ResourceAddress, fromAccount: AccountAddress): Resource =
        stateRepository.getResources(
            addresses = setOf(address),
            underAccountAddress = fromAccount,
            withDetails = false,
            withAllMetadata = false
        ).getOrNull()?.firstOrNull() ?: Resource.FungibleResource(address = address, ownedAmount = null)

    private val ResourceSpecifier.address: ResourceAddress
        get() = when (this) {
            is ResourceSpecifier.Fungible -> resourceAddress
            is ResourceSpecifier.NonFungible -> resourceAddress
        }
}
