package com.babylon.wallet.android.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.MainUiState
import com.babylon.wallet.android.presentation.account.AccountScreen
import com.babylon.wallet.android.presentation.accountpreference.accountPreferences
import com.babylon.wallet.android.presentation.accountpreference.accountPreferencesScreen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.createpersona.personasScreen
import com.babylon.wallet.android.presentation.createpersona.popPersonaCreation
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.completion.ChooseAccountsCompletionScreen
import com.babylon.wallet.android.presentation.dapp.success.requestResultSuccess
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.settings.dappdetail.dappDetailScreen
import com.babylon.wallet.android.presentation.settings.incompatibleprofile.IncompatibleProfileContent
import com.babylon.wallet.android.presentation.settings.incompatibleprofile.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.settings.personadetail.personaDetailScreen
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.settingsNavGraph
import com.babylon.wallet.android.presentation.transaction.transactionApprovalScreen
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.pager.ExperimentalPagerApi
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationHost(
    startDestination: String,
    navController: NavHostController,
    mainUiState: StateFlow<MainUiState>,
    onCloseApp: () -> Unit,
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.OnboardingDestination.route) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onOnBoardingEnd = {
                    navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
                },
                onBack = onCloseApp
            )
        }
        composable(route = Screen.WalletDestination.route) {
            WalletScreen(
                mainUiState = mainUiState,
                viewModel = hiltViewModel(),
                onMenuClick = {
                    navController.navigate(Screen.SettingsAllDestination.route)
                },
                onAccountClick = { accountId ->
                    navController.navigate(
                        Screen.AccountDestination.routeWithArgs(accountId)
                    )
                },
                onAccountCreationClick = {
                    navController.createAccountScreen(CreateAccountRequestSource.AccountsList)
                },
                onNavigateToCreateAccount = {
                    navController.createAccountScreen(CreateAccountRequestSource.FirstTime)
                },
                onNavigateToOnBoarding = {
                    navController.navigate(Screen.OnboardingDestination.route)
                },
                onNavigateToIncompatibleProfile = {
                    navController.navigate(ROUTE_INCOMPATIBLE_PROFILE)
                }
            )
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            }
        ) {
            AccountScreen(
                viewModel = hiltViewModel(),
                onAccountPreferenceClick = { address ->
                    navController.accountPreferences(address = address)
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
        createAccountScreen(
            startDestination = startDestination,
            onBackClick = {
                navController.navigateUp()
            },
            onContinueClick = { accountId, requestSource ->
                navController.createAccountConfirmationScreen(
                    accountId,
                    requestSource ?: CreateAccountRequestSource.FirstTime
                )
            },
            onCloseApp = onCloseApp
        )
        createAccountConfirmationScreen(
            onNavigateToWallet = {
                navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
            },
            onFinishAccountCreation = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, inclusive = true)
            }
        )
        createPersonaScreen(
            onBackClick = { navController.navigateUp() },
            onContinueClick = { personaId ->
                navController.createPersonaConfirmationScreen(personaId = personaId)
            }
        )
        personaInfoScreen(
            onBackClick = { navController.navigateUp() },
            onContinueClick = { navController.createPersonaScreen() }
        )
        personasScreen(
            onBackClick = { navController.navigateUp() },
            createPersonaScreen = {
                if (it) {
                    navController.createPersonaScreen()
                } else {
                    navController.personaInfoScreen()
                }
            },
            onPersonaClick = { personaAddress ->
                navController.personaDetailScreen(personaAddress)
            }
        )
        personaDetailScreen(
            onBackClick = {
                navController.navigateUp()
            },
            onPersonaEdit = {
                navController.personaEditScreen(it)
            },
            onDappClick = {
                navController.dappDetailScreen(it)
            }
        )
        personaEditScreen(onBackClick = {
            navController.navigateUp()
        })
        transactionApprovalScreen(onBackClick = {
            navController.popBackStack()
        })
        accountPreferencesScreen(onBackClick = {
            navController.popBackStack()
        })
        dAppLoginAuthorized(
            navController,
            onBackClick = {
                navController.popBackStack()
            },
            showSuccessDialog = { requestId, dAppName ->
                navController.requestResultSuccess(requestId, dAppName)
            }
        )
        dAppLoginUnauthorized(
            navController,
            onBackClick = {
                navController.popBackStack()
            },
            showSuccessDialog = { requestId, dAppName ->
                navController.requestResultSuccess(requestId, dAppName)
            }
        )
        settingsNavGraph(navController)
        requestResultSuccess(
            onBackPress = {
                navController.popBackStack()
            }
        )
        createPersonaConfirmationScreen(
            finishPersonaCreation = {
                navController.popPersonaCreation()
            }
        )
        composable(
            route = Screen.ChooseAccountsCompleteDestination.route + "/{${Screen.ARG_DAPP_NAME}}",
            arguments = listOf(
                navArgument(Screen.ARG_DAPP_NAME) { type = NavType.StringType }
            )
        ) {
            ChooseAccountsCompletionScreen(
                viewModel = hiltViewModel(),
                onContinueClick = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            route = ROUTE_INCOMPATIBLE_PROFILE
        ) {
            IncompatibleProfileContent(hiltViewModel(), onProfileDeleted = {
                navController.popBackStack(Screen.WalletDestination.route, false)
            })
        }
    }
}
