package com.babylon.wallet.android.presentation.ui.model.factors

import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class FactorSourceCard(
    val id: FactorSourceId,
    val name: String,
    val includeDescription: Boolean,
    val lastUsedOn: String?,
    val kind: FactorSourceKind,
    val messages: PersistentList<FactorSourceStatusMessage>,
    val accounts: PersistentList<Account>,
    val personas: PersistentList<Persona>,
    val hasHiddenEntities: Boolean
) {

    companion object {

        fun compact(
            id: FactorSourceId,
            name: String,
            kind: FactorSourceKind,
            includeDescription: Boolean = true
        ): FactorSourceCard = FactorSourceCard(
            id = id,
            name = name,
            includeDescription = includeDescription,
            lastUsedOn = null,
            kind = kind,
            messages = persistentListOf(),
            accounts = persistentListOf(),
            personas = persistentListOf(),
            hasHiddenEntities = false
        )
    }
}

@Suppress("LongParameterList")
fun FactorSource.toFactorSourceCard(
    includeDescription: Boolean = false,
    includeLastUsedOn: Boolean = true,
    messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
    accounts: PersistentList<Account> = persistentListOf(),
    personas: PersistentList<Persona> = persistentListOf(),
    hasHiddenEntities: Boolean = false,
): FactorSourceCard {
    return FactorSourceCard(
        id = this.id,
        name = when (this) {
            is FactorSource.ArculusCard -> this.value.hint.label
            is FactorSource.Device -> this.value.hint.label
            is FactorSource.Ledger -> this.value.hint.label
            is FactorSource.OffDeviceMnemonic -> this.value.hint.label.value
            is FactorSource.Password -> this.value.hint.label
            else -> ""
        },
        includeDescription = includeDescription,
        lastUsedOn = if (includeLastUsedOn) {
            when (this) {
                is FactorSource.ArculusCard -> this.value.common.lastUsedOn.relativeTimeFormatted()
                is FactorSource.Device -> this.value.common.lastUsedOn.relativeTimeFormatted()
                is FactorSource.Ledger -> this.value.common.lastUsedOn.relativeTimeFormatted()
                is FactorSource.OffDeviceMnemonic -> this.value.common.lastUsedOn.relativeTimeFormatted()
                is FactorSource.Password -> this.value.common.lastUsedOn.relativeTimeFormatted()
                else -> ""
            }
        } else {
            null
        },
        kind = kind,
        messages = messages,
        accounts = accounts,
        personas = personas,
        hasHiddenEntities = hasHiddenEntities
    )
}
