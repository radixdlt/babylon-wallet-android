package com.babylon.wallet.android.presentation.ui.model.securityshields

import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.SecurityStructureId
import kotlinx.collections.immutable.PersistentList

data class SecurityShieldCard(
    val id: SecurityStructureId,
    val name: DisplayName,
    val factorSources: PersistentList<FactorSource>
)
