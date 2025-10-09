package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrimaryRoleRecoveryAttempt(
    @SerialName("recovery_proposal")
    val recoveryProposal: RecoveryProposal?
)