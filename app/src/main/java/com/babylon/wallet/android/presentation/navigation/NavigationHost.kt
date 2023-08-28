package com.babylon.wallet.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.AccountScreen
import com.babylon.wallet.android.presentation.accountpreference.accountPreferences
import com.babylon.wallet.android.presentation.accountpreference.accountPreferencesScreen
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.confirmation.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createaccount.withledger.createAccountWithLedger
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.createpersona.personasScreen
import com.babylon.wallet.android.presentation.createpersona.popPersonaCreation
import com.babylon.wallet.android.presentation.dapp.authorized.dappLoginAuthorizedNavGraph
import com.babylon.wallet.android.presentation.dapp.completion.ChooseAccountsCompletionScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.dappLoginUnauthorizedNavGraph
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainUiState
import com.babylon.wallet.android.presentation.main.main
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.onboarding.restore.backup.restoreFromBackupScreen
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonicsScreen
import com.babylon.wallet.android.presentation.settings.incompatibleprofile.IncompatibleProfileContent
import com.babylon.wallet.android.presentation.settings.incompatibleprofile.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.settings.personadetail.personaDetailScreen
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.seedphrases.seedPhrases
import com.babylon.wallet.android.presentation.settings.settingsNavGraph
import com.babylon.wallet.android.presentation.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.transaction.transactionApprovalScreen
import com.babylon.wallet.android.presentation.transfer.transfer
import com.babylon.wallet.android.presentation.transfer.transferScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.domain.backup.BackupType

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
        composable(
            route = Screen.OnboardingDestination.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                onCreateNewWalletClick = {
                    navController.createAccountScreen(CreateAccountRequestSource.FirstTime)
                },
                onBack = onCloseApp,
                onRestoreFromBackupClick = {
                    navController.restoreFromBackupScreen()
                }
            )
        }
        restoreFromBackupScreen(
            onBack = {
                navController.popBackStack()
            },
            onRestoreConfirmed = { fromCloud ->
                navController.restoreMnemonics(
                    args = RestoreMnemonicsArgs.RestoreProfile(
                        backupType = if (fromCloud) BackupType.Cloud else BackupType.File.PlainText
                    )
                )
            }
        )
        restoreMnemonicsScreen(
            onCloseApp = onCloseApp,
            onDismiss = { isMovingToMain ->
                if (isMovingToMain) {
                    navController.popBackStack(MAIN_ROUTE, inclusive = false)
                } else {
                    navController.popBackStack()
                }
            },
            onCloseApp = onCloseApp
        )
        main(
            mainUiState = mainUiState,
            onMenuClick = {
                navController.navigate(Screen.SettingsAllDestination.route)
            },
            onAccountClick = { account ->
                navController.navigate(
                    Screen.AccountDestination.routeWithArgs(account.address)
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
            },
            onNavigateToMnemonicBackup = {
                navController.seedPhrases()
            },
            onNavigateToMnemonicRestore = {
                navController.restoreMnemonics(args = RestoreMnemonicsArgs.RestoreSpecificMnemonic(factorSourceId = it.body))
            },
        )
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType }
            )
        ) {
            AccountScreen(
                viewModel = hiltViewModel(),
                onAccountPreferenceClick = { address ->
                    navController.accountPreferences(address = address)
                },
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToMnemonicBackup = {
                    navController.seedPhrases()
                },
                onNavigateToMnemonicRestore = { factorSourceId ->
                    navController.restoreMnemonics(
                        args = RestoreMnemonicsArgs.RestoreSpecificMnemonic(factorSourceId = factorSourceId.body)
                    )
                },
                onTransferClick = { accountId ->
                    navController.transfer(accountId = accountId)
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
            onCloseApp = onCloseApp,
            onAddLedgerDevice = {
                navController.createAccountWithLedger()
            }
        )
        createAccountWithLedger(
            onBackClick = {
                navController.navigateUp()
            },
            goBackToCreateAccount = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, false)
            }
        )
        createAccountConfirmationScreen(
            onNavigateToWallet = {
                navController.popBackStack(MAIN_ROUTE, inclusive = false)
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
            },
            onNavigateToMnemonicBackup = {
                navController.seedPhrases()
            }
        )
        personaDetailScreen(
            onBackClick = {
                navController.navigateUp()
            },
            onPersonaEdit = {
                navController.personaEditScreen(it)
            }
        )
        personaEditScreen(onBackClick = {
            navController.navigateUp()
        })
        transactionApprovalScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        transferScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        accountPreferencesScreen {
            navController.popBackStack()
        }
        dappLoginAuthorizedNavGraph(navController = navController)
        dappLoginUnauthorizedNavGraph(navController = navController)
        settingsNavGraph(navController)
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
                navController.popBackStack(MAIN_ROUTE, false)
            })
        }
        dappInteractionDialog(
            onBackPress = {
                navController.popBackStack()
            }
        )
        transactionStatusDialog(
            onClose = {
                navController.popBackStack()
            }
        )
    }
}
