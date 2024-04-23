package com.babylon.wallet.android.presentation.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init
import rdx.works.core.HexCoded32Bytes

data class LedgerDeviceUiModel(
    val id: HexCoded32Bytes,
    val model: MessageFromDataChannel.LedgerResponse.LedgerDeviceModel,
    val name: String? = null
) {
    val factorSourceId: FactorSourceId.Hash
        get() = FactorSourceId.Hash(
            value = FactorSourceIdFromHash(
                kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                body = Exactly32Bytes.init(id.value.hexToBagOfBytes())
            )
        )
}
