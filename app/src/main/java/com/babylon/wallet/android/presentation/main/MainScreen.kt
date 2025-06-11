package com.babylon.wallet.android.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryScreen
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.DiscoverScreen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsScreen
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.babylon.wallet.android.designsystem.R as DSR

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    onAccountClick: (Account) -> Unit = { },
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onNavigateToOnBoarding: () -> Unit,
    onNavigateToIncompatibleProfile: () -> Unit,
    onNavigateToBootError: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoLinkClick: (GlossaryItem) -> Unit,
    onMoreInfoClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            MainContent(
                modifier = modifier,
                state = state,
                onTabClick = viewModel::onTabClick,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                showNPSSurvey = showNPSSurvey,
                onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
                onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
                onNavigateToLinkConnector = onNavigateToLinkConnector,
                onSettingClick = onSettingClick,
                onDAppClick = onDAppClick,
                onInfoLinkClick = onInfoLinkClick,
                onMoreInfoClick = onMoreInfoClick
            )
        }

        is AppState.IncompatibleProfile -> {
            LaunchedEffect(Unit) {
                onNavigateToIncompatibleProfile()
            }
        }

        is AppState.ErrorBootingSargon -> {
            LaunchedEffect(Unit) {
                onNavigateToBootError()
            }
        }

        is AppState.Loading -> {
            FullscreenCircularProgressContent()
        }

        is AppState.OnBoarding -> {
            LaunchedEffect(Unit) {
                onNavigateToOnBoarding()
            }
        }
    }
}

@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    state: MainViewModel.State,
    onTabClick: (MainTab) -> Unit,
    onAccountClick: (Account) -> Unit = { },
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onInfoLinkClick: (GlossaryItem) -> Unit,
    onMoreInfoClick: () -> Unit,
) {
    val bottomNavController = rememberNavController()

    LaunchedEffect(state.selectedTab) {
        if (bottomNavController.currentBackStackEntry?.destination?.route != state.selectedTab.route) {
            bottomNavController.navigate(state.selectedTab.route) {
                popUpTo(bottomNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        bottomNavController.currentBackStackEntryFlow.collect { entry ->
            when (entry.destination.route) {
                MainTab.Wallet.route -> onTabClick(MainTab.Wallet)
                MainTab.DApps.route -> onTabClick(MainTab.DApps)
                MainTab.Discover.route -> onTabClick(MainTab.Discover)
                MainTab.Settings.route -> onTabClick(MainTab.Settings)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                HorizontalDivider(color = RadixTheme.colors.divider)

                NavigationBar(
                    containerColor = RadixTheme.colors.background,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    state.tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == state.selectedTab,
                            colors = NavigationBarItemColors(
                                selectedIconColor = White,
                                selectedTextColor = RadixTheme.colors.text,
                                unselectedIconColor = RadixTheme.colors.icon,
                                unselectedTextColor = RadixTheme.colors.text,
                                disabledIconColor = RadixTheme.colors.backgroundTertiary,
                                disabledTextColor = RadixTheme.colors.backgroundTertiary,
                                selectedIndicatorColor = RadixTheme.colors.chipBackground
                            ),
                            label = {
                                Text(
                                    text = when (tab) {
                                        MainTab.Wallet -> stringResource(R.string.homePage_tab_wallet)
                                        MainTab.DApps -> stringResource(R.string.homePage_tab_dapps)
                                        MainTab.Discover -> stringResource(R.string.homePage_tab_discover)
                                        MainTab.Settings -> stringResource(R.string.homePage_tab_settings)
                                    }
                                )
                            },
                            onClick = {
                                onTabClick(tab)
                            },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(
                                        id = when (tab) {
                                            MainTab.Wallet -> DSR.drawable.ic_radix
                                            MainTab.DApps -> DSR.drawable.ic_authorized_dapps
                                            MainTab.Discover -> DSR.drawable.ic_authorized_dapps //TODO sergiu replace
                                            MainTab.Settings -> R.drawable.ic_home_settings
                                        }
                                    ),
                                    contentDescription = tab.name,
                                    tint = if (tab == state.selectedTab) {
                                        White
                                    } else {
                                        RadixTheme.colors.icon
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.none
    ) { padding ->
        NavHost(
            modifier = Modifier.padding(padding),
            navController = bottomNavController,
            startDestination = MainTab.Wallet.route
        ) {
            composable(MainTab.Wallet.route) {
                WalletScreen(
                    viewModel = hiltViewModel(),
                    onAccountClick = onAccountClick,
                    onAccountCreationClick = onAccountCreationClick,
                    onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                    showNPSSurvey = showNPSSurvey,
                    onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
                    onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
                    onNavigateToLinkConnector = onNavigateToLinkConnector,
                )
            }
            composable(MainTab.DApps.route) {
                DAppDirectoryScreen(
                    viewModel = hiltViewModel(),
                    onDAppClick = onDAppClick
                )
            }
            composable(MainTab.Discover.route) {
                DiscoverScreen(
                    viewModel = hiltViewModel(),
                    onInfoClick = onInfoLinkClick,
                    onMoreInfoClick = onMoreInfoClick
                )
            }
            composable(MainTab.Settings.route) {
                SettingsScreen(
                    viewModel = hiltViewModel(),
                    onSettingClick = onSettingClick,
                )
            }
        }
    }
}
