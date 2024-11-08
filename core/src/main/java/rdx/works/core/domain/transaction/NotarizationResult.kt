package rdx.works.core.domain.transaction

import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.NotarizedTransaction
import com.radixdlt.sargon.extensions.hash

data class NotarizationResult(
    val endEpoch: Epoch,
    val notarizedTransaction: NotarizedTransaction
) {

    val intentHash = notarizedTransaction.signedIntent.intent.hash()
}
