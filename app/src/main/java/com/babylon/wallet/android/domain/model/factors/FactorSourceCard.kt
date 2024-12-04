package com.babylon.wallet.android.domain.model.factors

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona

data class FactorSourceCard(
    val kind: FactorSourceKind,
    val header: Header,
    val messages: List<StatusMessage>,
    val accounts: List<Account>,
    val personas: List<Persona>
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