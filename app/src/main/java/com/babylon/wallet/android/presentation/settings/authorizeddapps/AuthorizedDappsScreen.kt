package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.card.DappCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthorizedDAppsScreen(
    viewModel: AuthorizedDappsViewModel,
    onBackClick: () -> Unit,
    onDAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AuthorizedDAppsContent(
        onBackClick = onBackClick,
        dApps = state.dApps,
        onDAppClick = onDAppClick,
        modifier = modifier
    )
}

@Composable
private fun AuthorizedDAppsContent(
    onBackClick: () -> Unit,
    dApps: ImmutableList<DAppWithMetadataAndAssociatedResources>,
    onDAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(R.string.authorizedDapps_title),
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Divider(color = RadixTheme.colors.gray5)
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            Text(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.authorizedDapps_subtitle),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // TODO enable it when we have the link
//                InfoLink(stringResource(R.string.authorizedDapps_whatIsDapp), modifier = Modifier.fillMaxWidth())
//                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                items(dApps) { dApp ->
                    DappCard(
                        modifier = Modifier.throttleClickable {
                            onDAppClick(dApp.dAppWithMetadata.dAppAddress)
                        },
                        dApp = dApp.dAppWithMetadata
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthorizedDAppsContentPreview() {
    RadixWalletTheme {
        AuthorizedDAppsContent(
            onBackClick = {},
            dApps = listOf(SampleDataProvider().sampleDAppWithResources()).toImmutableList(),
            onDAppClick = {}
        )
    }
}
