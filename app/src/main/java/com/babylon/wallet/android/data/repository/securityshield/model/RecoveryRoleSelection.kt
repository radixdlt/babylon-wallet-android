package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource

data class RecoveryRoleSelection(
    val startRecoveryFactors: List<FactorSource>,
    val confirmationFactors: List<FactorSource>,
    val numberOfDaysUntilAutoConfirm: Int
)
