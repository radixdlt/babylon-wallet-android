package com.babylon.wallet.android.presentation.dappdir.all

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListPreviewProvider
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import com.babylon.wallet.android.presentation.dappdir.common.views.DAppListFiltersView
import com.babylon.wallet.android.presentation.dappdir.common.views.DAppListView
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues

@Composable
fun AllDAppsView(
    modifier: Modifier = Modifier,
    viewModel: AllDAppsViewModel,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state: DAppListState by viewModel.state.collectAsStateWithLifecycle()

    AllDAppsContent(
        modifier = modifier,
        state = state,
        onSearchTermUpdated = viewModel::onSearchTermUpdated,
        onFilterTagAdded = viewModel::onFilterTagAdded,
        onFilterTagRemoved = viewModel::onFilterTagRemoved,
        onAllFilterTagsRemoved = viewModel::onAllFilterTagsRemoved,
        onRefresh = viewModel::onRefresh,
        onMessageShown = viewModel::onMessageShown,
        onDAppClick = onDAppClick,
        onInfoClick = onInfoClick
    )
}

@Composable
private fun AllDAppsContent(
    state: DAppListState,
    modifier: Modifier = Modifier,
    onSearchTermUpdated: (String) -> Unit,
    onFilterTagAdded: (String) -> Unit,
    onFilterTagRemoved: (String) -> Unit,
    onAllFilterTagsRemoved: () -> Unit,
    onRefresh: () -> Unit,
    onMessageShown: () -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            DAppListFiltersView(
                filters = state.filters,
                isLoading = state.isLoading,
                isFiltersButtonEnabled = state.isFiltersButtonEnabled,
                onSearchTermUpdated = onSearchTermUpdated,
                onFilterTagAdded = onFilterTagAdded,
                onFilterTagRemoved = onFilterTagRemoved,
                onAllFilterTagsRemoved = onAllFilterTagsRemoved
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        DAppListView(
            padding = padding,
            state = state,
            emptyStateTitle = stringResource(id = R.string.dappDirectory_empty_message),
            onRefresh = onRefresh,
            onDAppClick = onDAppClick,
            onInfoClick = onInfoClick
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun AllDAppsPreviewLight(
    @PreviewParameter(DAppListPreviewProvider::class) state: DAppListState
) {
    RadixWalletPreviewTheme {
        AllDAppsContent(
            state = state,
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {},
            onInfoClick = {}
        )
    }
}

@Preview
@Composable
@UsesSampleValues
private fun AllDAppsPreviewDark(
    @PreviewParameter(DAppListPreviewProvider::class) state: DAppListState
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        AllDAppsContent(
            state = state,
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {},
            onInfoClick = {}
        )
    }
}
