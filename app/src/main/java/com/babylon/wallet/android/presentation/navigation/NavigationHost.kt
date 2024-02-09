package com.babylon.wallet.android.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.accessfactorsources.accessFactorSources
import com.babylon.wallet.android.presentation.account.AccountScreen
import com.babylon.wallet.android.presentation.account.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.account.recover.complete.recoveryScanComplete
import com.babylon.wallet.android.presentation.account.recover.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.account.settings.AccountSettingItem
import com.babylon.wallet.android.presentation.account.settings.accountSettings
import com.babylon.wallet.android.presentation.account.settings.devsettings.devSettings
import com.babylon.wallet.android.presentation.account.settings.specificassets.specificAssets
import com.babylon.wallet.android.presentation.account.settings.specificdepositor.specificDepositor
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.accountThirdPartyDeposits
import com.babylon.wallet.android.presentation.dapp.authorized.dappLoginAuthorizedNavGraph
import com.babylon.wallet.android.presentation.dapp.completion.ChooseAccountsCompletionScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.dappLoginUnauthorizedNavGraph
import com.babylon.wallet.android.presentation.incompatibleprofile.IncompatibleProfileContent
import com.babylon.wallet.android.presentation.incompatibleprofile.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainUiState
import com.babylon.wallet.android.presentation.main.main
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ADDRESS
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.onboarding.eula.eulaScreen
import com.babylon.wallet.android.presentation.onboarding.eula.navigateToEulaScreen
import com.babylon.wallet.android.presentation.onboarding.restore.backup.ROUTE_RESTORE_FROM_BACKUP
import com.babylon.wallet.android.presentation.onboarding.restore.backup.restoreFromBackupScreen
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonicsScreen
import com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup.restoreWithoutBackupScreen
import com.babylon.wallet.android.presentation.rootdetection.ROUTE_ROOT_DETECTION
import com.babylon.wallet.android.presentation.rootdetection.RootDetectionContent
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal.ROUTE_REVEAL_SEED_PHRASE
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.seedPhrases
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personasScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.popPersonaCreation
import com.babylon.wallet.android.presentation.settings.personas.personadetail.personaDetailScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.settingsNavGraph
import com.babylon.wallet.android.presentation.status.assets.assetDialog
import com.babylon.wallet.android.presentation.status.assets.fungibleAssetDialog
import com.babylon.wallet.android.presentation.status.assets.nftAssetDialog
import com.babylon.wallet.android.presentation.status.dapp.dAppDetailsDialog
import com.babylon.wallet.android.presentation.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.transaction.transactionReviewScreen
import com.babylon.wallet.android.presentation.transfer.transfer
import com.babylon.wallet.android.presentation.transfer.transferScreen
import com.radixdlt.ret.Address
import kotlinx.coroutines.flow.StateFlow
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.backup.BackupType

@Suppress("CyclomaticComplexMethod")
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    mainUiState: StateFlow<MainUiState>,
    startDestination: String,
    navController: NavHostController,
    onCloseApp: () -> Unit,
) {
    NavHost(
        modifier = modifier,
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
                    navController.navigateToEulaScreen()
                },
                onBack = onCloseApp,
                onRestoreFromBackupClick = {
                    navController.restoreFromBackupScreen()
                }
            )
        }
        eulaScreen(
            onBack = {
                navController.popBackStack()
            },
            onAccepted = {
                navController.createAccountScreen(CreateAccountRequestSource.FirstTime)
            }
        )
        restoreFromBackupScreen(
            onBack = {
                navController.popBackStack()
            },
            onRestoreConfirmed = { fromCloud ->
                navController.restoreMnemonics(
                    args = RestoreMnemonicsArgs(
                        backupType = if (fromCloud) BackupType.Cloud else BackupType.File.PlainText
                    )
                )
            },
            onOtherRestoreOptionsClick = {
                navController.restoreWithoutBackupScreen()
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
            }
        )
        addSingleMnemonic(
            navController = navController,
            onBackClick = {
                navController.popBackStack()
            },
            onStartRecovery = {
                navController.accountRecoveryScan()
            }
        )
        restoreWithoutBackupScreen(
            onBack = { navController.popBackStack() },
            onRestoreConfirmed = {
                navController.addSingleMnemonic(mnemonicType = MnemonicType.BabylonMain)
            },
            onNewUserConfirmClick = {
                navController.popBackStack(ROUTE_RESTORE_FROM_BACKUP, inclusive = true)
            }
        )
        accountRecoveryScan(
            navController = navController,
            onBackClick = {
                navController.popBackStack()
            },
            onRecoveryComplete = {
                navController.recoveryScanComplete()
            }
        )
        confirmSeedPhrase(onMnemonicBackedUp = {
            navController.popBackStack(ROUTE_REVEAL_SEED_PHRASE, inclusive = true)
        }, onDismiss = {
            navController.popBackStack()
        })
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
            onNavigateToMnemonicBackup = {
                navController.seedPhrases()
            },
            onNavigateToMnemonicRestore = {
                navController.restoreMnemonics(
                    args = RestoreMnemonicsArgs()
                )
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.AccountsList)
            },
            onNavigateToOnBoarding = {
                navController.navigate(Screen.OnboardingDestination.route)
            }
        ) {
            navController.navigate(ROUTE_INCOMPATIBLE_PROFILE)
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ADDRESS}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ADDRESS) { type = NavType.StringType }
            )
        ) {
            AccountScreen(
                viewModel = hiltViewModel(),
                onAccountPreferenceClick = { address ->
                    navController.accountSettings(address = address)
                },
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToMnemonicBackup = {
                    navController.seedPhrases()
                },
                onNavigateToMnemonicRestore = {
                    navController.restoreMnemonics(
                        args = RestoreMnemonicsArgs()
                    )
                },
                onFungibleResourceClick = { resource, account ->
                    val resourceWithAmount = resource.ownedAmount?.let {
                        mapOf(resource.resourceAddress to it)
                    }.orEmpty()
                    navController.fungibleAssetDialog(
                        resourceAddress = resource.resourceAddress,
                        amounts = resourceWithAmount,
                        underAccountAddress = account.address
                    )
                },
                onNonFungibleResourceClick = { resource, item, account ->
                    navController.nftAssetDialog(
                        resourceAddress = resource.resourceAddress,
                        localId = item.localId.code,
                        underAccountAddress = account.address
                    )
                },
                onTransferClick = { accountId ->
                    navController.transfer(accountId = accountId)
                }
            )
        }
        accessFactorSources(
            onDismiss = {
                navController.popBackStack()
            }
        )
        createAccountScreen(
            onBackClick = {
                navController.navigateUp()
            },
            onContinueClick = { accountId, requestSource ->
                navController.createAccountConfirmationScreen(
                    accountId,
                    requestSource ?: CreateAccountRequestSource.FirstTime
                )
            },
            onAddLedgerDevice = {
                navController.chooseLedger(networkId = it)
            }
        )
        chooseLedger(
            onBackClick = {
                navController.navigateUp()
            },
            onFinish = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, false)
            },
            onStartRecovery = { factorSource, isOlympia ->
                navController.accountRecoveryScan(factorSource.identifier, isOlympia)
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
                navController.revealSeedPhrase(it.body.value)
            }
        )
        personaDetailScreen(
            onBackClick = {
                navController.navigateUp()
            },
            onPersonaEdit = {
                navController.personaEditScreen(it)
            },
            onDAppClick = {
                navController.dAppDetailsDialog(dAppDefinitionAddress = it.dAppAddress)
            }
        )
        personaEditScreen(onBackClick = {
            navController.navigateUp()
        })
        transactionReviewScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onTransferableFungibleClick = { asset ->
                val resourcesWithAmount = when (asset) {
                    is TransferableAsset.Fungible.LSUAsset -> {
                        val xrdResourceAddress = runCatching {
                            val networkId = Address(asset.resource.resourceAddress).networkId().toInt()
                            XrdResource.address(networkId = NetworkId.from(networkId))
                        }.getOrNull()

                        mutableMapOf(
                            asset.resource.resourceAddress to asset.amount,
                        ).apply {
                            if (xrdResourceAddress != null) {
                                put(xrdResourceAddress, asset.xrdWorth)
                            }
                        }
                    }
                    is TransferableAsset.Fungible.PoolUnitAsset -> mutableMapOf(asset.resource.resourceAddress to asset.amount).apply {
                        putAll(asset.contributionPerResource)
                    }
                    is TransferableAsset.Fungible.Token -> mapOf(asset.resource.resourceAddress to asset.amount)
                }
                navController.fungibleAssetDialog(
                    resourceAddress = asset.resource.resourceAddress,
                    amounts = resourcesWithAmount,
                    isNewlyCreated = asset.isNewlyCreated
                )
            },
            onTransferableNonFungibleClick = { asset, item ->
                navController.nftAssetDialog(
                    resourceAddress = asset.resource.resourceAddress,
                    localId = item.localId.code,
                    isNewlyCreated = asset.isNewlyCreated
                )
            },
            onDAppClick = { dApp ->
                navController.dAppDetailsDialog(dAppDefinitionAddress = dApp.dAppAddress)
            }
        )
        transferScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        accountSettings(
            onBackClick = {
                navController.popBackStack()
            },
            onAccountSettingItemClick = { item, accountAddress ->
                when (item) {
                    AccountSettingItem.ThirdPartyDeposits -> {
                        navController.accountThirdPartyDeposits(accountAddress)
                    }

                    AccountSettingItem.DevSettings -> {
                        navController.devSettings(accountAddress)
                    }

                    else -> {}
                }
            },
            onHideAccountClick = {
                navController.popBackStack(MAIN_ROUTE, inclusive = false)
            }
        )
        devSettings(onBackClick = {
            navController.popBackStack()
        })
        accountThirdPartyDeposits(
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onAssetSpecificRulesClick = {
                navController.specificAssets()
            },
            onSpecificDepositorsClick = {
                navController.specificDepositor()
            }
        )
        specificAssets(navController = navController, onBackClick = {
            navController.navigateUp()
        })
        specificDepositor(navController = navController, onBackClick = {
            navController.navigateUp()
        })
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
        composable(
            route = ROUTE_ROOT_DETECTION
        ) {
            RootDetectionContent(
                viewModel = hiltViewModel(),
                onAcknowledgeDeviceRooted = {
                    navController.popBackStack(MAIN_ROUTE, false)
                },
                onCloseApp = onCloseApp
            )
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
        assetDialog(
            onDismiss = {
                navController.popBackStack()
            }
        )
        dAppDetailsDialog(
            onFungibleClick = {
                navController.fungibleAssetDialog(resourceAddress = it.resourceAddress)
            },
            onNonFungibleClick = {
                navController.nftAssetDialog(resourceAddress = it.resourceAddress)
            },
            onDismiss = {
                navController.popBackStack()
            }
        )
        recoveryScanComplete(
            onContinueClick = {
                navController.popBackStack(MAIN_ROUTE, false)
            }
        )
    }
}
