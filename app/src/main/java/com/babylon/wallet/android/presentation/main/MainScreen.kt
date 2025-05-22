package com.babylon.wallet.android.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    onNavigateToDAppDirectory: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDAppClick: (AccountAddress) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.initialAppState) {
        is AppState.Wallet -> {
            MainContent(
                modifier = modifier,
                onAccountClick = onAccountClick,
                onAccountCreationClick = onAccountCreationClick,
                onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                showNPSSurvey = showNPSSurvey,
                onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
                onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
                onNavigateToLinkConnector = onNavigateToLinkConnector,
                onNavigateToDAppDirectory = onNavigateToDAppDirectory,
                onSettingClick = onSettingClick,
                onDAppClick = onDAppClick
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
    onAccountClick: (Account) -> Unit = { },
    onNavigateToSecurityCenter: () -> Unit,
    onAccountCreationClick: () -> Unit,
    showNPSSurvey: () -> Unit,
    onNavigateToRelinkConnectors: () -> Unit,
    onNavigateToConnectCloudBackup: () -> Unit,
    onNavigateToLinkConnector: () -> Unit,
    onNavigateToDAppDirectory: () -> Unit,
    onSettingClick: (SettingsItem.TopLevelSettings) -> Unit,
    onDAppClick: (AccountAddress) -> Unit
) {
    val bottomNavController = rememberNavController()
    var selectedTab by remember {
        mutableStateOf(MainTab.Wallet)
    }

    LaunchedEffect(Unit) {
        bottomNavController.currentBackStackEntryFlow.collect { entry ->
            when (entry.destination.route) {
                MainTab.Wallet.route -> selectedTab = MainTab.Wallet
                MainTab.Discover.route -> selectedTab = MainTab.Discover
                MainTab.Settings.route -> selectedTab = MainTab.Settings
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
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            colors = NavigationBarItemColors(
                                selectedIconColor = White,
                                selectedTextColor = RadixTheme.colors.text,
                                unselectedIconColor = RadixTheme.colors.icon,
                                unselectedTextColor = RadixTheme.colors.text,
                                disabledIconColor = RadixTheme.colors.backgroundTertiary,
                                disabledTextColor = RadixTheme.colors.backgroundTertiary,
                                selectedIndicatorColor = if (RadixTheme.config.isDarkTheme) {
                                    RadixTheme.colors.backgroundTertiary
                                } else {
                                    RadixTheme.colors.icon
                                }
                            ),
                            label = {
                                Text(
                                    text = when (tab) {
                                        MainTab.Wallet -> "Wallet"
                                        MainTab.Discover -> "Discover"
                                        MainTab.Settings -> "Settings"
                                    }
                                )
                            },
                            onClick = {
                                bottomNavController.navigate(tab.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(id = when (tab) {
                                        MainTab.Wallet -> DSR.drawable.ic_radix
                                        MainTab.Discover -> DSR.drawable.ic_authorized_dapps
                                        MainTab.Settings -> R.drawable.ic_home_settings
                                    }),
                                    contentDescription = tab.name,
                                    tint = if (tab == selectedTab) {
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
                    modifier = modifier,
                    viewModel = hiltViewModel(),
                    onAccountClick = onAccountClick,
                    onAccountCreationClick = onAccountCreationClick,
                    onNavigateToSecurityCenter = onNavigateToSecurityCenter,
                    showNPSSurvey = showNPSSurvey,
                    onNavigateToRelinkConnectors = onNavigateToRelinkConnectors,
                    onNavigateToConnectCloudBackup = onNavigateToConnectCloudBackup,
                    onNavigateToLinkConnector = onNavigateToLinkConnector,
                    onNavigateToDAppDirectory = onNavigateToDAppDirectory
                )
            }
            composable(MainTab.Discover.route) {
                DAppDirectoryScreen(
                    viewModel = hiltViewModel(),
                    onDAppClick = onDAppClick
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

private enum class MainTab(val route: String) {
    Wallet("tab_wallet"),
    Discover("tab_discover"),
    Settings("tab_settings")
}