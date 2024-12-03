package com.babylon.wallet.android.domain.model.factors

import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.Persona

data class FactorSourceCard(
    val kind: FactorSourceKind,
    val lastUsedOn: String?,
    val messages: List<StatusMessage>,
    val accounts: List<Account>,
    val personas: List<Persona>
)