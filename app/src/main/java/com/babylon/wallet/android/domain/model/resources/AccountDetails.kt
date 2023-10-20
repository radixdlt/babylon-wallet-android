package com.babylon.wallet.android.domain.model.resources

import com.babylon.wallet.android.domain.model.resources.metadata.AccountTypeMetadataItem

data class AccountDetails(
    private val typeMetadataItem: AccountTypeMetadataItem? = null
) {
    val type: AccountTypeMetadataItem.AccountType?
        get() = typeMetadataItem?.type
}
