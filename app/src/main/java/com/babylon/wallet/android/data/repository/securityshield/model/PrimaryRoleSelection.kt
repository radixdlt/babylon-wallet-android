package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus
import com.radixdlt.sargon.Threshold

data class PrimaryRoleSelection(
    val threshold: Threshold = Threshold.All,
    val thresholdValues: List<Threshold> = emptyList(),
    val thresholdFactors: List<FactorSource> = emptyList(),
    val overrideFactors: List<FactorSource> = emptyList(),
    val authenticationFactor: FactorSource? = null,
    val primaryRoleStatus: SelectedPrimaryThresholdFactorsStatus = SelectedPrimaryThresholdFactorsStatus.Optimal,
    val shieldStatus: SecurityShieldBuilderInvalidReason? = null
)
