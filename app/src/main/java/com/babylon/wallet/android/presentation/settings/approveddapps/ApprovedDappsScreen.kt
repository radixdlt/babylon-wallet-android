package com.babylon.wallet.android.presentation.settings.approveddapps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.card.DappCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.toImmutableList
import rdx.works.core.domain.DApp

@Composable
fun ApprovedDAppsScreen(
    modifier: Modifier = Modifier,
    viewModel: ApprovedDappsViewModel,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApprovedDAppsContent(
        modifier = modifier,
        state = state,
        onMessageShown = viewModel::onMessageShown,
        onDAppClick = onDAppClick,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick
    )
}

@Composable
private fun ApprovedDAppsContent(
    modifier: Modifier = Modifier,
    state: AuthorizedDappsUiState,
    onDAppClick: (AccountAddress) -> Unit,
    onMessageShown: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.walletSettings_dapps_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column {
                HorizontalDivider(color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingMedium
                    ),
                    text = stringResource(R.string.authorizedDapps_subtitle),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray2
                )
                InfoButton(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingMedium
                    ),
                    text = stringResource(id = R.string.authorizedDapps_whatIsDapp),
                    onClick = {
                        onInfoClick(GlossaryItem.dapps)
                    }
                )
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingLarge
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.dApps) { dApp ->
                        DappCard(
                            modifier = Modifier.throttleClickable {
                                onDAppClick(dApp.dAppAddress)
                            },
                            dApp = dApp
                        )
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RadixTheme.colors.gray1
                )
            }
        }
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ApprovedDAppsContentPreview() {
    RadixWalletTheme {
        ApprovedDAppsContent(
            onBackClick = {},
            state = AuthorizedDappsUiState(
                dApps = DApp.sampleMainnet.all.toImmutableList()
            ),
            onDAppClick = {},
            onMessageShown = {},
            onInfoClick = {}
        )
    }
}
