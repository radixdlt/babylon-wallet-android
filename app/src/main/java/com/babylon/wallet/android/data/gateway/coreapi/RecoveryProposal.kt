package com.babylon.wallet.android.data.gateway.coreapi

import com.babylon.wallet.android.data.gateway.model.AccessRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecoveryProposal(
    @SerialName("primary_role")
    val primaryRole: AccessRule? = null,

    @SerialName("recovery_role")
    val recoveryRole: AccessRule? = null,

    @SerialName("confirmation_role")
    val confirmationRole: AccessRule? = null,

    @SerialName("timed_recovery_delay_minutes")
    val timedRecoveryDelayMinutes: Long? = null
)