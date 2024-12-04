package com.babylon.wallet.android.domain.model.factors

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona
import kotlinx.collections.immutable.PersistentList

data class FactorSourceCard(
    val kind: FactorSourceKind,
    val header: Header,
    val messages: PersistentList<StatusMessage>,
    val accounts: PersistentList<Account>,
    val personas: PersistentList<Persona>
) {

    sealed interface Header {

        data object New : Header

        data class Added(
            val id: FactorSourceId.Hash,
            val name: String,
            val lastUsedOn: String?
        ) : Header
    }
}
