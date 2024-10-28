package rdx.works.core.domain.transaction

import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.TransactionIntentHash

data class NotarizationResult(
    val intentHash: TransactionIntentHash,
    val endEpoch: Epoch,
    val notarizedTransaction: NotarizedTransaction
)
