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
import com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts.deriveAccounts
import com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey.derivePublicKeyDialog
import com.babylon.wallet.android.presentation.accessfactorsources.signatures.getSignatures
import com.babylon.wallet.android.presentation.account.account
import com.babylon.wallet.android.presentation.account.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.account.history.history
import com.babylon.wallet.android.presentation.account.settings.AccountSettingItem
import com.babylon.wallet.android.presentation.account.settings.accountSettings
import com.babylon.wallet.android.presentation.account.settings.devsettings.devSettings
import com.babylon.wallet.android.presentation.account.settings.specificassets.specificAssets
import com.babylon.wallet.android.presentation.account.settings.specificdepositor.specificDepositor
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.accountThirdPartyDeposits
import com.babylon.wallet.android.presentation.dapp.authorized.dappLoginAuthorizedNavGraph
import com.babylon.wallet.android.presentation.dapp.authorized.login.dAppLoginAuthorized
import com.babylon.wallet.android.presentation.dapp.completion.ChooseAccountsCompletionScreen
import com.babylon.wallet.android.presentation.dapp.unauthorized.dappLoginUnauthorizedNavGraph
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.dAppLoginUnauthorized
import com.babylon.wallet.android.presentation.incompatibleprofile.IncompatibleProfileScreen
import com.babylon.wallet.android.presentation.incompatibleprofile.ROUTE_INCOMPATIBLE_PROFILE
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.main.MainUiState
import com.babylon.wallet.android.presentation.main.main
import com.babylon.wallet.android.presentation.mobileconnect.ROUTE_MOBILE_CONNECT
import com.babylon.wallet.android.presentation.mobileconnect.mobileConnect
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.onboarding.cloudbackup.ConnectCloudBackupViewModel.ConnectMode
import com.babylon.wallet.android.presentation.onboarding.cloudbackup.connectCloudBackupScreen
import com.babylon.wallet.android.presentation.onboarding.eula.eulaScreen
import com.babylon.wallet.android.presentation.onboarding.eula.navigateToEulaScreen
import com.babylon.wallet.android.presentation.onboarding.restore.backup.restoreFromBackupScreen
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.MnemonicType
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonicsScreen
import com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup.restoreWithoutBackupScreen
import com.babylon.wallet.android.presentation.rootdetection.ROUTE_ROOT_DETECTION
import com.babylon.wallet.android.presentation.rootdetection.RootDetectionContent
import com.babylon.wallet.android.presentation.settings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.linkedconnectors.relink.relinkConnectors
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personaInfoScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personasScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.popPersonaCreation
import com.babylon.wallet.android.presentation.settings.personas.personadetail.personaDetailScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityCenter
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal.ROUTE_REVEAL_SEED_PHRASE
import com.babylon.wallet.android.presentation.settings.settingsNavGraph
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scancomplete.recoveryScanComplete
import com.babylon.wallet.android.presentation.status.address.addressDetails
import com.babylon.wallet.android.presentation.status.assets.assetDialog
import com.babylon.wallet.android.presentation.status.assets.fungibleAssetDialog
import com.babylon.wallet.android.presentation.status.assets.nftAssetDialog
import com.babylon.wallet.android.presentation.status.dapp.dAppDetailsDialog
import com.babylon.wallet.android.presentation.status.dapp.dappInteractionDialog
import com.babylon.wallet.android.presentation.status.transaction.transactionStatusDialog
import com.babylon.wallet.android.presentation.survey.npsSurveyDialog
import com.babylon.wallet.android.presentation.transaction.transactionReview
import com.babylon.wallet.android.presentation.transaction.transactionReviewScreen
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.transfer
import com.babylon.wallet.android.presentation.transfer.transferScreen
import com.babylon.wallet.android.presentation.walletclaimed.claimedByAnotherDevice
import com.radixdlt.sargon.extensions.networkId
import kotlinx.coroutines.flow.StateFlow
import rdx.works.core.domain.resources.XrdResource

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
                onCreateNewWalletClick = {
                    navController.navigateToEulaScreen()
                },
                onBack = onCloseApp,
                onRestoreFromBackupClick = {
                    navController.connectCloudBackupScreen(connectMode = ConnectMode.RestoreWallet)
                }
            )
        }
        eulaScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onAccepted = { isWithCloudBackupEnabled ->
                if (isWithCloudBackupEnabled) {
                    navController.createAccountScreen()
                } else {
                    navController.connectCloudBackupScreen(connectMode = ConnectMode.NewWallet)
                }
            }
        )
        connectCloudBackupScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onProceed = { mode, isCloudBackupEnabled ->
                when (mode) {
                    ConnectMode.NewWallet -> if (isCloudBackupEnabled) {
                        navController.createAccountScreen(CreateAccountRequestSource.FirstTimeWithCloudBackupEnabled)
                    } else {
                        navController.createAccountScreen(CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled)
                    }

                    ConnectMode.RestoreWallet -> navController.restoreFromBackupScreen()
                    ConnectMode.ExistingWallet -> navController.popBackStack()
                }
            }
        )
        restoreFromBackupScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onRestoreConfirmed = {
                navController.restoreMnemonics(args = RestoreMnemonicsArgs(backupType = it))
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
                navController.popBackStack(Screen.OnboardingDestination.route, inclusive = false)
            }
        )
        accountRecoveryScan(
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
                navController.account(accountAddress = account.address)
            },
            onNavigateToSecurityCenter = {
                navController.securityCenter()
            },
            onAccountCreationClick = {
                navController.createAccountScreen(CreateAccountRequestSource.AccountsList)
            },
            onNavigateToOnBoarding = {
                navController.navigate(Screen.OnboardingDestination.route)
            },
            onNavigateToIncompatibleProfile = {
                navController.navigate(ROUTE_INCOMPATIBLE_PROFILE)
            },
            showNPSSurvey = {
                navController.npsSurveyDialog()
            },
            onNavigateToRelinkConnectors = {
                navController.relinkConnectors()
            },
            onNavigateToConnectCloudBackup = {
                navController.connectCloudBackupScreen(connectMode = ConnectMode.ExistingWallet)
            },
            onNavigateToLinkConnector = {
                navController.linkedConnectorsScreen(shouldShowAddLinkConnectorScreen = true)
            }
        )
        account(
            onAccountPreferenceClick = { address ->
                navController.accountSettings(address = address)
            },
            onBackClick = {
                navController.navigateUp()
            },
            onNavigateToSecurityCenter = {
                navController.securityCenter()
            },
            onFungibleResourceClick = { resource, account ->
                val resourceWithAmount = resource.ownedAmount?.let {
                    mapOf(resource.address to it)
                }.orEmpty()
                navController.fungibleAssetDialog(
                    resourceAddress = resource.address,
                    amounts = resourceWithAmount,
                    underAccountAddress = account.address
                )
            },
            onNonFungibleResourceClick = { resource, item, account ->
                navController.nftAssetDialog(
                    resourceAddress = resource.address,
                    localId = item.localId,
                    underAccountAddress = account.address
                )
            },
            onTransferClick = { accountId ->
                navController.transfer(accountId = accountId)
            },
            onHistoryClick = { accountAddress ->
                navController.history(accountAddress)
            }
        )
        derivePublicKeyDialog(
            onDismiss = {
                navController.popBackStack()
            }
        )
        deriveAccounts(
            onDismiss = {
                navController.popBackStack()
            }
        )
        getSignatures(
            onDismiss = {
                navController.popBackStack()
            }
        )
        history(
            onBackClick = {
                navController.navigateUp()
            }
        )
        createAccountScreen(
            onBackClick = {
                navController.navigateUp()
            },
            onContinueClick = { accountId, requestSource ->
                navController.createAccountConfirmationScreen(
                    accountId = accountId,
                    requestSource = requestSource ?: CreateAccountRequestSource.FirstTimeWithCloudBackupDisabled
                )
            },
            onAddLedgerDevice = {
                navController.chooseLedger()
            }
        )
        chooseLedger(
            onBackClick = {
                navController.navigateUp()
            },
            onFinish = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, false)
            },
            onStartRecovery = { factorSourceId, isOlympia ->
                navController.accountRecoveryScan(
                    factorSourceId = factorSourceId,
                    isOlympia = isOlympia
                )
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
            onBackClick = { navController.navigateUp() }
        ) {
            navController.createPersonaConfirmationScreen()
        }
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
            onNavigateToSecurityCenter = {
                navController.securityCenter()
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
                            val networkId = asset.resourceAddress.networkId
                            XrdResource.address(networkId = networkId)
                        }.getOrNull()

                        mutableMapOf(
                            asset.resource.address to asset.amount,
                        ).apply {
                            if (xrdResourceAddress != null) {
                                put(xrdResourceAddress, asset.xrdWorth)
                            }
                        }
                    }

                    is TransferableAsset.Fungible.PoolUnitAsset -> mutableMapOf(asset.resource.address to asset.amount).apply {
                        putAll(asset.contributionPerResource)
                    }

                    is TransferableAsset.Fungible.Token -> mapOf(asset.resource.address to asset.amount)
                }
                navController.fungibleAssetDialog(
                    resourceAddress = asset.resource.address,
                    amounts = resourcesWithAmount,
                    isNewlyCreated = asset.isNewlyCreated
                )
            },
            onTransferableNonFungibleClick = { asset, item ->
                navController.nftAssetDialog(
                    resourceAddress = asset.resource.address,
                    localId = item?.localId,
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
            },
            onShowAssetDetails = { spendingAsset, fromAccount ->
                when (spendingAsset) {
                    is SpendingAsset.Fungible -> navController.fungibleAssetDialog(
                        resourceAddress = spendingAsset.resourceAddress,
                        amounts = spendingAsset.resource.ownedAmount?.let { mapOf(spendingAsset.resourceAddress to it) }.orEmpty(),
                        underAccountAddress = fromAccount.address
                    )
                    is SpendingAsset.NFT -> navController.nftAssetDialog(
                        resourceAddress = spendingAsset.resourceAddress,
                        localId = spendingAsset.item.localId,
                        underAccountAddress = null // Marking as null hides claim button when the nft is a claim
                    )
                }
            }
        )
        accountSettings(
            onBackClick = {
                navController.popBackStack()
            },
            onAccountSettingItemClick = { item, accountAddress ->
                when (item) {
                    is AccountSettingItem.ThirdPartyDeposits -> {
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
            IncompatibleProfileScreen(
                viewModel = hiltViewModel(),
                onProfileDeleted = {
                    navController.popBackStack(MAIN_ROUTE, false)
                }
            )
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
        claimedByAnotherDevice(
            onNavigateToOnboarding = {
                navController.popBackStack(MAIN_ROUTE, false)
            },
            onReclaimedBack = {
                navController.popBackStack()
            }
        )
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
        npsSurveyDialog(
            onDismiss = {
                navController.popBackStack()
            }
        )
        dAppDetailsDialog(
            onFungibleClick = {
                navController.fungibleAssetDialog(resourceAddress = it.address)
            },
            onNonFungibleClick = {
                navController.nftAssetDialog(resourceAddress = it.address)
            },
            onDismiss = {
                navController.popBackStack()
            }
        )
        addressDetails(
            onDismiss = {
                navController.popBackStack()
            }
        )
        recoveryScanComplete(
            onContinueClick = {
                navController.popBackStack(MAIN_ROUTE, false)
            }
        )
        relinkConnectors(
            onContinueClick = { popUpToRoute ->
                navController.linkedConnectorsScreen(
                    shouldShowAddLinkConnectorScreen = true,
                    popUpToRoute = popUpToRoute
                )
            },
            onDismiss = {
                navController.popBackStack()
            }
        )
        mobileConnect(
            onBackClick = {
                navController.popBackStack()
            },
            onHandleRequestAuthorizedRequest = {
                navController.dAppLoginAuthorized(it) {
                    popUpTo(ROUTE_MOBILE_CONNECT) { inclusive = true }
                }
            },
            onHandleUnauthorizedRequest = {
                navController.dAppLoginUnauthorized(it) {
                    popUpTo(ROUTE_MOBILE_CONNECT) { inclusive = true }
                }
            },
            onHandleTransactionRequest = {
                navController.transactionReview(it) {
                    popUpTo(ROUTE_MOBILE_CONNECT) { inclusive = true }
                }
            }
        )
    }
}
