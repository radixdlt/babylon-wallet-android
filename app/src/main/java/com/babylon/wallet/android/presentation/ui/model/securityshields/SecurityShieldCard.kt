package com.babylon.wallet.android.presentation.ui.model.securityshields

import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.ShieldForDisplay
import kotlinx.collections.immutable.PersistentList

data class SecurityShieldCard(
    val shieldForDisplay: ShieldForDisplay,
    val messages: PersistentList<SecurityShieldStatusMessage>
) {
    val id: SecurityStructureId = shieldForDisplay.metadata.id
}
