package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import com.radixdlt.sargon.SelectedPrimaryThresholdFactorsStatus

data class PrimaryRoleSelection(
    val threshold: Int,
    val thresholdFactors: List<FactorSource>,
    val overrideFactors: List<FactorSource>,
    val authenticationFactor: FactorSource?,
    val primaryRoleStatus: SelectedPrimaryThresholdFactorsStatus,
    val shieldStatus: SecurityShieldBuilderInvalidReason?
)
