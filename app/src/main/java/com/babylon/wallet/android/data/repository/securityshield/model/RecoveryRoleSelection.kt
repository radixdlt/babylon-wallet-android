package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityShieldBuilderStatus
import com.radixdlt.sargon.TimePeriod

data class RecoveryRoleSelection(
    val startRecoveryFactors: List<FactorSource>,
    val confirmationFactors: List<FactorSource>,
    val timePeriodUntilAutoConfirm: TimePeriod,
    val shieldStatus: SecurityShieldBuilderStatus?
)
