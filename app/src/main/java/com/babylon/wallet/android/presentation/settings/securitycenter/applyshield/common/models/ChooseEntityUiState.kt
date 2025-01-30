package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.models

import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.presentation.common.UiState

data class ChooseEntityUiState<T>(
    val items: List<Selectable<T>> = emptyList()
) : UiState {

    val isEmpty = items.isEmpty()
    val selectedAll = items.all { it.selected }
    val isButtonEnabled = items.any { it.selected } || isEmpty
}
