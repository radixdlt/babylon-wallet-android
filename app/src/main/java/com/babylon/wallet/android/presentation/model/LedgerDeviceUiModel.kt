package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.core.HexCoded32Bytes

data class LedgerDeviceUiModel(
    val id: HexCoded32Bytes,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
)
