package com.babylon.wallet.android.presentation.ui.model.factors

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class FactorSourceInstanceCard(
    val id: FactorSourceId,
    val name: String,
    val includeDescription: Boolean,
    val lastUsedOn: String?,
    val kind: FactorSourceKind,
    val messages: PersistentList<FactorSourceStatusMessage>,
    val accounts: PersistentList<Account>,
    val personas: PersistentList<Persona>
) {

    companion object {

        fun compact(
            id: FactorSourceId,
            name: String,
            kind: FactorSourceKind
        ): FactorSourceInstanceCard = FactorSourceInstanceCard(
            id = id,
            name = name,
            includeDescription = false,
            lastUsedOn = null,
            kind = kind,
            messages = persistentListOf(),
            accounts = persistentListOf(),
            personas = persistentListOf()
        )
    }
}
