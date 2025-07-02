@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.babylon.wallet.android.presentation.dappdir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dappdir.all.AllDAppsView
import com.babylon.wallet.android.presentation.dappdir.all.AllDAppsViewModel
import com.babylon.wallet.android.presentation.dappdir.connected.ConnectedDAppsView
import com.babylon.wallet.android.presentation.dappdir.connected.ConnectedDAppsViewModel
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AccountAddress

@Composable
fun DAppDirectoryScreen(
    modifier: Modifier = Modifier,
    viewModel: DAppDirectoryViewModel,
    allDAppsViewModel: AllDAppsViewModel,
    connectedDAppsViewModel: ConnectedDAppsViewModel,
    onDAppClick: (address: AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state: DAppDirectoryViewModel.State by viewModel.state.collectAsStateWithLifecycle()

    DAppDirectoryContent(
        modifier = modifier,
        state = state,
        onTabSelected = viewModel::onTabSelected
    ) {
        when (state.selectedTab) {
            DAppDirectoryViewModel.State.Tab.All -> {
                AllDAppsView(
                    viewModel = allDAppsViewModel,
                    onDAppClick = onDAppClick,
                    onInfoClick = onInfoClick
                )
            }

            DAppDirectoryViewModel.State.Tab.Connected -> {
                ConnectedDAppsView(
                    viewModel = connectedDAppsViewModel,
                    onDAppClick = onDAppClick,
                    onInfoClick = onInfoClick
                )
            }
        }
    }
}

@Composable
private fun DAppDirectoryContent(
    modifier: Modifier = Modifier,
    state: DAppDirectoryViewModel.State,
    onTabSelected: (DAppDirectoryViewModel.State.Tab) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.background)
                    .padding(bottom = RadixTheme.dimensions.paddingDefault)
            ) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.dappDirectory_title),
                    onBackClick = {},
                    backIconType = BackIconType.None,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                val tabIndex = remember(state.selectedTab) {
                    DAppDirectoryViewModel.State.Tab.entries.indexOf(state.selectedTab)
                }

                TabRow(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .clip(shape = RadixTheme.shapes.roundedRectSmall),
                    selectedTabIndex = tabIndex,
                    containerColor = RadixTheme.colors.unselectedSegmentedControl,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[tabIndex])
                                .fillMaxHeight()
                                .zIndex(-1f)
                                .padding(2.dp)
                                .background(
                                    color = RadixTheme.colors.selectedSegmentedControl,
                                    shape = RadixTheme.shapes.roundedRectSmall
                                )
                        )
                    }
                ) {
                    DAppDirectoryViewModel.State.Tab.entries.forEach { tab ->
                        val isSelected = tab == state.selectedTab
                        Tab(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    onTabSelected(tab)
                                }
                            },
                            selectedContentColor = RadixTheme.colors.text,
                            unselectedContentColor = RadixTheme.colors.textSecondary
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    horizontal = RadixTheme.dimensions.paddingMedium,
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                                text = tab.title(),
                                style = if (isSelected) {
                                    RadixTheme.typography.body1Header
                                } else {
                                    RadixTheme.typography.body1Regular
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Box(
            modifier = Modifier.padding(
                top = padding.calculateTopPadding()
            )
        ) {
            content()
        }
    }
}

@Composable
private fun DAppDirectoryViewModel.State.Tab.title() = when (this) {
    DAppDirectoryViewModel.State.Tab.All -> stringResource(R.string.dappDirectory_viewAllDApps)
    DAppDirectoryViewModel.State.Tab.Connected -> stringResource(R.string.dappDirectory_viewConnectedDApps)
}

@Preview
@Composable
private fun DAppDirectoryPreviewLight(
    @PreviewParameter(DAppDirectoryPreviewProvider::class) state: DAppDirectoryViewModel.State
) {
    RadixWalletPreviewTheme {
        DAppDirectoryContent(
            state = state,
            onTabSelected = {},
            content = {}
        )
    }
}

@Preview
@Composable
private fun DAppDirectoryPreviewDark(
    @PreviewParameter(DAppDirectoryPreviewProvider::class) state: DAppDirectoryViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DAppDirectoryContent(
            state = state,
            onTabSelected = {},
            content = {}
        )
    }
}

class DAppDirectoryPreviewProvider : PreviewParameterProvider<DAppDirectoryViewModel.State> {

    override val values: Sequence<DAppDirectoryViewModel.State>
        get() = sequenceOf(
            DAppDirectoryViewModel.State(
                selectedTab = DAppDirectoryViewModel.State.Tab.All
            ),
            DAppDirectoryViewModel.State(
                selectedTab = DAppDirectoryViewModel.State.Tab.Connected
            )
        )
}
