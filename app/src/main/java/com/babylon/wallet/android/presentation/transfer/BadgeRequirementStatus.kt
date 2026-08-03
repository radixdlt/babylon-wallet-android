package com.babylon.wallet.android.presentation.transfer

import com.radixdlt.sargon.ResourceAddress
import rdx.works.core.domain.resources.Resource

sealed interface BadgeRequirementStatus {
    data object None : BadgeRequirementStatus
    data object Loading : BadgeRequirementStatus
    data class Success(val resource: Resource?) : BadgeRequirementStatus {
        constructor(badgeAddress: ResourceAddress) : this(
            resource = Resource.FungibleResource(
                address = badgeAddress,
                ownedAmount = null
            )
        )
        val badgeAddress: ResourceAddress
            get() = resource?.address ?: throw IllegalStateException("Resource has no address")
    }
    data class Error(val resource: Resource?, val reason: Reason) : BadgeRequirementStatus {
        constructor(badgeAddress: ResourceAddress, reason: Reason) : this(
            resource = Resource.FungibleResource(
                address = badgeAddress,
                ownedAmount = null
            ),
            reason = reason
        )
        val badgeAddress: ResourceAddress
            get() = resource?.address ?: throw IllegalStateException("Resource has no address")
        enum class Reason {
            MissingBadge,
            InsufficientBalance,
            RuleParsingError
        }
    }
}


