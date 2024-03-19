package rdx.works.core.domain.resources

import rdx.works.core.domain.resources.metadata.AccountType
import java.time.Instant

data class AccountDetails(
    val stateVersion: Long,
    val accountType: AccountType? = null,
    val firstTransactionDate: Instant? = null
)
