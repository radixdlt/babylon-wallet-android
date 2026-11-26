package com.babylon.wallet.android.presentation.transfer

import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.resources.Resource

/**
 * Whether the account can withdraw the restricted tokens of a transfer (see
 * `GetTransferBadgeRequirementsUseCase`).
 */
sealed interface BadgeRequirementStatus {

    /** No transferred token is restricted. */
    data object None : BadgeRequirementStatus

    data object Loading : BadgeRequirementStatus

    /** The account holds the badges: their proofs are added to the transfer. [resource] is the first badge. */
    data class Success(val resource: Resource?) : BadgeRequirementStatus

    /**
     * [restrictedResource] is the restricted token or NFT that cannot be transferred, so that the message
     * names what it really is; null when the rules could not be checked.
     */
    data class Error(
        val resource: Resource?,
        val reason: Reason,
        val restrictedResource: ResourceAddress? = null
    ) : BadgeRequirementStatus {
        enum class Reason {
            /** The account does not hold the badge [resource]. */
            MissingBadge,

            /** Nobody may withdraw the token [resource]. */
            NotWithdrawable,

            /** The rules could not be checked (e.g. no connection). */
            CheckFailed
        }
    }
}
