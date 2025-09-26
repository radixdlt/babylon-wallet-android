package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models

import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.UiState

data class ChooseEntityUiState<T>(
    val items: List<Selectable<T>> = emptyList(),
    val mustSelectOne: Boolean = true,
    val canSkip: Boolean = false
) : UiState {

    val isEmpty = items.isEmpty()
    val isButtonEnabled = when {
        mustSelectOne -> items.any { it.selected }
        else -> true
    }
}
