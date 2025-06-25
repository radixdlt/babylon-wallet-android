package com.babylon.wallet.android.presentation.dappdir.common.models

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.Category
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet

data class DAppListState(
    val isLoading: Boolean,
    val isRefreshing: Boolean = false,
    val errorLoading: Boolean = false,
    val items: List<DAppListItem> = emptyList(),
    val filters: DAppFilters = DAppFilters(),
    val uiMessage: UiMessage? = null,
) : UiState {

    val isEmpty: Boolean = items.isEmpty() && !isLoading && !errorLoading && filters.searchTerm.isBlank() &&
        filters.selectedTags.isEmpty()
    val isFiltersButtonVisible: Boolean = !isLoading && filters.availableTags.isNotEmpty()
}

class DAppListPreviewProvider : PreviewParameterProvider<DAppListState> {

    override val values: Sequence<DAppListState>
        get() = sequenceOf(
            DAppListState(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                    Category(
                        type = DAppCategoryType.Unknown
                    ),
                    DAppWithDetails.sample.other()
                ),
                filters = DAppFilters(),
                uiMessage = null
            ),
            DAppListState(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                ),
                filters = DAppFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            DAppListState(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                ),
                filters = DAppFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            DAppListState(
                isLoading = false,
                isRefreshing = false,
                errorLoading = true,
                items = emptyList(),
                filters = DAppFilters(),
                uiMessage = null
            ),
            DAppListState(
                isLoading = true,
                isRefreshing = false,
                errorLoading = false,
                items = emptyList(),
                filters = DAppFilters(),
                uiMessage = null
            ),
            DAppListState(
                isLoading = false,
                isRefreshing = false,
                errorLoading = false,
                items = emptyList(),
                filters = DAppFilters(),
                uiMessage = null
            )
        )
}

@UsesSampleValues
val DAppWithDetails.Companion.sample: Sample<DAppWithDetails>
    get() = object : Sample<DAppWithDetails> {

        override fun invoke(): DAppWithDetails = DAppWithDetails(
            dAppDefinitionAddress = AccountAddress.sampleMainnet(),
            hasDeposits = false,
            details = DAppWithDetails.Details.Data(
                name = "Awesome DApp",
                description = "Awesome DApp is an awesome dApp for trading on Radix.",
                iconUri = null,
                tags = setOf("defi", "fungible", "token")
            )
        )

        override fun other(): DAppWithDetails = DAppWithDetails(
            dAppDefinitionAddress = AccountAddress.sampleMainnet.other(),
            hasDeposits = false,
            details = DAppWithDetails.Details.Data(
                name = "Dashboard",
                description = "Explore assets and transactions on Radix",
                iconUri = null,
                tags = emptySet()
            )
        )
    }
