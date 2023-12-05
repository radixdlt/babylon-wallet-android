package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.resources.metadata.AccountType
import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem

data class AccountDetails(
    val stateVersion: Long,
    private val typeMetadataItem: AccountTypeMetadataItem? = null
) {
    val type: AccountType?
        get() = typeMetadataItem?.type
}
