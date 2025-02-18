package com.babylon.wallet.android.presentation.ui.model.securityshields

import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.ShieldForDisplay
import kotlinx.collections.immutable.PersistentList

data class SecurityShieldCard(
    val shieldForDisplay: ShieldForDisplay,
    val messages: PersistentList<SecurityShieldStatusMessage>
) {
    val id: SecurityStructureId = shieldForDisplay.metadata.id

    val numberOfLinkedAccounts: Int = shieldForDisplay.numberOfLinkedAccounts.toInt()

    val numberOfLinkedPersonas: Int = shieldForDisplay.numberOfLinkedPersonas.toInt()

    val hasAnyHiddenLinkedEntities: Boolean = shieldForDisplay.numberOfLinkedHiddenAccounts.toInt() != 0 ||
        shieldForDisplay.numberOfLinkedHiddenPersonas.toInt() != 0
}
