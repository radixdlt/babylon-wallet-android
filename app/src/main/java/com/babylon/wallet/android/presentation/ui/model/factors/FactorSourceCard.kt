package com.babylon.wallet.android.presentation.ui.model.factors

import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.sargon.lastUsedOn

data class FactorSourceCard(
    val id: FactorSourceId,
    val name: String,
    val includeDescription: Boolean,
    val lastUsedOn: String?,
    val kind: FactorSourceKind,
    val messages: PersistentList<FactorSourceStatusMessage>,
    val accounts: PersistentList<Account>,
    val personas: PersistentList<Persona>,
    val hasHiddenEntities: Boolean,
    val supportsBabylon: Boolean,
    val supportsOlympia: Boolean,
    val isEnabled: Boolean
)

@Suppress("LongParameterList")
fun FactorSource.toFactorSourceCard(
    includeDescription: Boolean = false,
    includeLastUsedOn: Boolean = true,
    messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
    accounts: PersistentList<Account> = persistentListOf(),
    personas: PersistentList<Persona> = persistentListOf(),
    hasHiddenEntities: Boolean = false,
    isEnabled: Boolean = true
): FactorSourceCard {
    return FactorSourceCard(
        id = this.id,
        name = when (this) {
            is FactorSource.ArculusCard -> this.value.hint.label
            is FactorSource.Device -> this.value.hint.label
            is FactorSource.Ledger -> this.value.hint.label
            is FactorSource.OffDeviceMnemonic -> this.value.hint.label.value
            is FactorSource.Password -> this.value.hint.label
        },
        includeDescription = includeDescription,
        lastUsedOn = if (includeLastUsedOn) {
            lastUsedOn.relativeTimeFormatted()
        } else {
            null
        },
        kind = kind,
        messages = messages,
        accounts = accounts,
        personas = personas,
        hasHiddenEntities = hasHiddenEntities,
        supportsBabylon = supportsBabylon,
        supportsOlympia = supportsOlympia,
        isEnabled = isEnabled
    )
}
