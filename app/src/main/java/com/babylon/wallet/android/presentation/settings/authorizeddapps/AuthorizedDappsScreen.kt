package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppResources
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.StandardOneLineCard
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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
            LazyColumn(
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.authorizedDapps_subtitle),
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.gray2
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    // TODO enable it when we have the link
//                InfoLink(stringResource(R.string.authorizedDapps_whatIsDapp), modifier = Modifier.fillMaxWidth())
//                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                items(dApps) { dApp ->
                    StandardOneLineCard(
                        image = dApp.dAppWithMetadata.iconUrl.toString(),
                        title = dApp.dAppWithMetadata.displayName(),
                        modifier = Modifier
                            .shadow(elevation = 8.dp, shape = RadixTheme.shapes.roundedRectMedium)
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onDAppClick(dApp.dAppWithMetadata.dAppAddress)
                            }
                            .fillMaxWidth()
                            .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingDefault
                            )
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
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
            dApps = persistentListOf(),
            onDAppClick = {}
        )
    }
}
