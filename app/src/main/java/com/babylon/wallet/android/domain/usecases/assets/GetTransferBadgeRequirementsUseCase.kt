package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.transfer.BadgeRequirementStatus
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.RestrictedWithdrawalStatus
import com.radixdlt.sargon.TransferRequiredBadges
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import rdx.works.core.domain.resources.Resource
import javax.inject.Inject

/**
 * The badges a transfer of restricted tokens needs. Sargon reads the withdraw rules of the
 * transferred resources and what the account holds ([TransferRequiredBadges]); this only adds
 * the badges' names and icons for the person.
 */
class GetTransferBadgeRequirementsUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val stateRepository: StateRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    data class Result(
        val status: BadgeRequirementStatus,
        /** The badges to prove in the transfer manifest. */
        val requiredBadges: List<ResourceSpecifier>
    )

    suspend operator fun invoke(
        fromAccount: AccountAddress,
        resources: Set<ResourceAddress>
    ): kotlin.Result<Result> = sargonOsManager.callSafely(dispatcher) {
        transferRequiredBadges(fromAccount = fromAccount, resources = resources.toList())
    }.map { outcome ->
        Result(status = outcome.toStatus(fromAccount), requiredBadges = outcome.requiredBadges)
    }

    private suspend fun TransferRequiredBadges.toStatus(fromAccount: AccountAddress): BadgeRequirementStatus {
        if (restrictedWithdrawals.isEmpty()) return BadgeRequirementStatus.None

        val unsatisfied = restrictedWithdrawals.firstOrNull { it.status != RestrictedWithdrawalStatus.SATISFIED }
        return when {
            // Met by the account's own signature alone: no badge to show.
            unsatisfied == null && requiredBadges.isEmpty() -> BadgeRequirementStatus.None

            unsatisfied == null -> BadgeRequirementStatus.Success(
                resource = requiredBadges.firstOrNull()?.let { resourceForDisplay(it.address, fromAccount) }
            )

            unsatisfied.status == RestrictedWithdrawalStatus.NOT_WITHDRAWABLE || unsatisfied.badges.isEmpty() ->
                BadgeRequirementStatus.Error(
                    resource = resourceForDisplay(unsatisfied.resource, fromAccount),
                    reason = BadgeRequirementStatus.Error.Reason.NotWithdrawable,
                    restrictedResource = unsatisfied.resource
                )

            else -> BadgeRequirementStatus.Error(
                resource = resourceForDisplay(unsatisfied.badges.first(), fromAccount),
                reason = BadgeRequirementStatus.Error.Reason.MissingBadge,
                restrictedResource = unsatisfied.resource
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
