package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityShieldBuilderInvalidReason
import com.radixdlt.sargon.SelectedFactorSourcesForRoleStatus

data class PrimaryRoleSelection(
    val threshold: Int,
    val thresholdFactors: List<FactorSource>,
    val overrideFactors: List<FactorSource>,
    val loginFactor: FactorSource?,
    val primaryRoleStatus: SelectedFactorSourcesForRoleStatus,
    val shieldStatus: SecurityShieldBuilderInvalidReason?
)
