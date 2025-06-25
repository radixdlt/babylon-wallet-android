package com.babylon.wallet.android.presentation.dappdir.common.models

import android.net.Uri
import com.radixdlt.sargon.AccountAddress

sealed interface DAppListItem {

    data class Category(
        val type: DAppCategoryType
    ) : DAppListItem

    data class DAppWithDetails(
        val dAppDefinitionAddress: AccountAddress,
        val hasDeposits: Boolean,
        val details: Details
    ) : DAppListItem {

        val isFetchingDAppDetails: Boolean = details is Details.Fetching
        val data: Details.Data? = details as? Details.Data

        sealed interface Details {

            data object Fetching : Details

            data object Error : Details

            data class Data(
                val name: String,
                val iconUri: Uri?,
                val description: String?,
                val tags: Set<String>
            ) : Details
        }

        companion object
    }
}
