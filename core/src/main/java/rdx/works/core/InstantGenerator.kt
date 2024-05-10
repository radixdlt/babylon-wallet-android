package rdx.works.core

import com.radixdlt.sargon.Timestamp
import java.time.Instant

object InstantGenerator {

    operator fun invoke(): Instant = Instant.now()
}

object TimestampGenerator {

    operator fun invoke(): Timestamp = Timestamp.now()
}
