package rdx.works.core.domain.resources

import rdx.works.core.domain.resources.metadata.AccountType

data class AccountDetails(
    val stateVersion: Long,
    val accountType: AccountType? = null
)
