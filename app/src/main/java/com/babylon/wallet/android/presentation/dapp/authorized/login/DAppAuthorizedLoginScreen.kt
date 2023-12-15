package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.DappInteractionFailureDialog
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.status.signing.FactorSourceInteractionBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DappAuthorizedLoginScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    navigateToChooseAccount: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToPermissions: (Int, Boolean, Boolean, Boolean) -> Unit,
    navigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    navigateToSelectPersona: (String) -> Unit,
    navigateToOngoingPersonaData: (String, RequiredPersonaFields) -> Unit,
    onLoginFlowComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    HandleOneOffEvents(viewModel.oneOffEvent, onBackClick, onLoginFlowComplete)
    val sharedState by viewModel.state.collectAsStateWithLifecycle()
    val initialRoute = sharedState.initialAuthorizedLoginRoute
    BackHandler {
        viewModel.onAbortDappLogin()
    }
    LaunchedEffect(initialRoute) {
        snapshotFlow { initialRoute }.distinctUntilChanged().collect { route ->
            if (route == InitialAuthorizedLoginRoute.CompleteRequest) {
                viewModel.completeRequestHandling(deviceBiometricAuthenticationProvider = {
                    context.biometricAuthenticateSuspend()
                }, abortOnFailure = true)
            }
        }
    }
    if (sharedState.isNoMnemonicErrorVisible) {
        BasicPromptAlertDialog(
            finish = {
                viewModel.dismissNoMnemonicError()
            },
            title = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            text = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val route = state.initialAuthorizedLoginRoute) {
        is InitialAuthorizedLoginRoute.ChooseAccount -> navigateToChooseAccount(
            route.numberOfAccounts,
            route.isExactAccountsCount,
            route.oneTime,
            route.showBack
        )

        is InitialAuthorizedLoginRoute.OneTimePersonaData -> navigateToOneTimePersonaData(route.requiredPersonaFields)
        is InitialAuthorizedLoginRoute.OngoingPersonaData -> navigateToOngoingPersonaData(
            route.personaAddress,
            route.requiredPersonaFields
        )

        is InitialAuthorizedLoginRoute.Permission -> navigateToPermissions(
            route.numberOfAccounts,
            route.isExactAccountsCount,
            route.oneTime,
            route.showBack
        )

        is InitialAuthorizedLoginRoute.SelectPersona -> navigateToSelectPersona(route.dappDefinitionAddress)
        else -> {}
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                backIconType = BackIconType.Close,
                onBackClick = viewModel::onAbortDappLogin,
                windowInsets = WindowInsets.statusBars
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RadixTheme.colors.defaultBackground)
        ) {
            AnimatedVisibility(
                visible = state.initialAuthorizedLoginRoute == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                FullscreenCircularProgressContent()
            }
            DappInteractionFailureDialog(
                dialogState = state.failureDialog,
                onAcknowledgeFailureDialog = viewModel::onAcknowledgeFailureDialog
            )

            SnackbarUiMessageHandler(
                message = state.uiMessage,
                onMessageShown = viewModel::onMessageShown,
                modifier = Modifier.imePadding()
            )
            sharedState.interactionState?.let {
                FactorSourceInteractionBottomDialog(
                    modifier = Modifier.fillMaxHeight(0.8f),
                    onDismissDialogClick = viewModel::onDismissSigningStatusDialog,
                    interactionState = it
                )
            }
        }
    }
}

@Composable
private fun HandleOneOffEvents(
    events: Flow<Event>,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                Event.CloseLoginFlow -> onBackClick()
                Event.LoginFlowCompleted -> onLoginFlowComplete()
                else -> {}
            }
        }
    }
}
