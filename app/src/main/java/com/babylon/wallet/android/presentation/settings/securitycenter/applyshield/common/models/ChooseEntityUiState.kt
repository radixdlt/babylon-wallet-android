package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models

import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.UiState

data class ChooseEntityUiState<T>(
    val items: List<Selectable<T>> = emptyList(),
    val isButtonEnabled: Boolean = items.any { it.selected } || items.isEmpty()
) : UiState {

    val isEmpty = items.isEmpty()
    val selectedAll = items.all { it.selected }
}
