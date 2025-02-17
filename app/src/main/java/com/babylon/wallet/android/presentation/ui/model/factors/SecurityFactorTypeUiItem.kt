package com.babylon.wallet.android.presentation.ui.model.factors

import com.babylon.wallet.android.domain.model.FactorSourceCategory
import com.radixdlt.sargon.FactorSourceKind
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

sealed interface SecurityFactorTypeUiItem {

    data class Header(
        val category: FactorSourceCategory
    ) : SecurityFactorTypeUiItem

    data class Item(
        val factorSourceKind: FactorSourceKind,
        val messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        val isEnabled: Boolean = true
    ) : SecurityFactorTypeUiItem
}
