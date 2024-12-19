package com.babylon.wallet.android.data.repository.securityshield.model

import com.radixdlt.sargon.FactorSource

data class PrimaryRoleSelection(
    val threshold: Int,
    val thresholdFactors: List<FactorSource>,
    val overrideFactors: List<FactorSource>,
    val loginFactor: FactorSource?
)