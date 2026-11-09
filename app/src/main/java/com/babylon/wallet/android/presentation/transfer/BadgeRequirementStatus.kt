package com.babylon.wallet.android.presentation.transfer

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

    data class Error(val resource: Resource?, val reason: Reason) : BadgeRequirementStatus {
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
