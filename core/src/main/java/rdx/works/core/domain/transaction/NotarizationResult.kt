package rdx.works.core.domain.transaction

import com.radixdlt.sargon.CompiledNotarizedIntent
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.IntentHash

data class NotarizationResult(
    val intentHash: IntentHash,
    val compiledNotarizedIntent: CompiledNotarizedIntent,
    val endEpoch: Epoch
)
