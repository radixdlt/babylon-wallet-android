package com.babylon.wallet.android.presentation.ui.model.factors

import com.radixdlt.sargon.FactorSourceKind
import kotlinx.collections.immutable.PersistentList

data class FactorSourceKindCard(
    val kind: FactorSourceKind,
    val messages: PersistentList<FactorSourceStatusMessage>
)
