package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecoveryRoleRecoveryAttempt(
    @SerialName("recovery_proposal")
    val recoveryProposal: RecoveryProposal? = null,

    @SerialName("allow_timed_recovery_after")
    val allowTimedRecoveryAfter: ScryptoInstant? = null
)