package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.profile.data.model.factorsources.FactorSource

data class LedgerDeviceUiModel(
    val id: FactorSource.HexCoded32Bytes,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
)
