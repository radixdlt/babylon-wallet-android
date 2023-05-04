package com.babylon.wallet.android.domain.model

data class LedgerDeviceUiModel(
    val id: String,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
)
