package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind

data class LedgerDeviceUiModel(
    val id: Exactly32Bytes,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
) {
    val factorSourceId: FactorSourceId.Hash
        get() = FactorSourceId.Hash(
            value = FactorSourceIdFromHash(
                kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                body = id
            )
        )
}
