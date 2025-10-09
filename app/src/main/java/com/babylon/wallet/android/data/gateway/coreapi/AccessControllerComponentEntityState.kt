package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessControllerComponentEntityState(
    @SerialName(value = "primary_role_recovery_attempt")
    val primaryRoleRecoveryAttempt: PrimaryRoleRecoveryAttempt? = null,

    @SerialName(value = "recovery_role_recovery_attempt")
    val recoveryRoleRecoveryAttempt: RecoveryRoleRecoveryAttempt? = null,
)