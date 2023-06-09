package rdx.works.core

import java.time.Instant

object InstantGenerator {

    operator fun invoke(): Instant = Instant.now()
}
