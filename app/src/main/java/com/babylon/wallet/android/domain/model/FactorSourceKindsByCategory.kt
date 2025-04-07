package com.babylon.wallet.android.domain.model

import com.radixdlt.sargon.FactorSourceKind

data class FactorSourceKindsByCategory(
    val category: FactorSourceCategory,
    val kinds: List<FactorSourceKind>
)
