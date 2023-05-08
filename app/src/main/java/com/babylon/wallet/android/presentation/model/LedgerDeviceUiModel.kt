package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel

data class LedgerDeviceUiModel(
    val id: String,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
)
