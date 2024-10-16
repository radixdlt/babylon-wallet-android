package rdx.works.core.domain.transaction

import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.NotarizedTransaction

data class NotarizationResult(
    val intentHash: IntentHash,
    val endEpoch: Epoch,
    val notarizedTransaction: NotarizedTransaction
)
