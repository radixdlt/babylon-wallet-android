package rdx.works.core.domain.resources

import rdx.works.core.domain.resources.metadata.AccountType
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.accountType
import java.time.Instant

data class AccountDetails(
    val stateVersion: Long,
    val metadata: List<Metadata>,
    val firstTransactionDate: Instant? = null
) {

    val accountType: AccountType?
        get() = metadata.accountType()

}
