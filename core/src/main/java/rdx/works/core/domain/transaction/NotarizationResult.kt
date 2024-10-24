package rdx.works.core.domain.transaction

import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.NotarizedTransaction

data class NotarizationResult(
    val intentHash: TransactionIntentHash,
    val endEpoch: Epoch,
    val notarizedTransaction: NotarizedTransaction
)
