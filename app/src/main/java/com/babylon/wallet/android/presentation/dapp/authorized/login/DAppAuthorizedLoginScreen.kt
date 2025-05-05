package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.DappInteractionFailureDialog
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.IdentityAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DappAuthorizedLoginScreen(
    viewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    onNavigateToSelectPersona: (authorizedRequestInteractionId: String, dappDefinitionAddress: AccountAddress) -> Unit,
    onNavigateToOneTimeAccounts: (
        authorizedRequestInteractionId: String,
        isOneTimeRequest: Boolean,
        isExactAccountsCount: Boolean,
        numberOfAccounts: Int,
        showBacK: Boolean
    ) -> Unit,
    onNavigateToOngoingAccounts: (
        isOneTimeRequest: Boolean,
        isExactAccountsCount: Boolean,
        numberOfAccounts: Int,
        showBacK: Boolean
    ) -> Unit,
    onNavigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    onNavigateToOngoingPersonaData: (IdentityAddress, RequiredPersonaFields) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onLoginFlowComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by viewModel.state.collectAsStateWithLifecycle()
    val initialRoute = sharedState.initialAuthorizedLoginRoute

    HandleOneOffEvents(
        events = viewModel.oneOffEvent,
        onBackClick = onBackClick,
        onLoginFlowComplete = onLoginFlowComplete
    )

    BackHandler {
        viewModel.onAbortDappLogin()
    }

    LaunchedEffect(initialRoute) {
        snapshotFlow { initialRoute }.distinctUntilChanged().collect { route ->
            if (route == InitialAuthorizedLoginRoute.CompleteRequest) {
                viewModel.completeRequestHandling()
            }
        }
    }

    if (sharedState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            viewModel.dismissNoMnemonicError()
        }
    }

    LaunchedEffect(state.initialAuthorizedLoginRoute) {
        when (val route = state.initialAuthorizedLoginRoute) {
            is InitialAuthorizedLoginRoute.SelectPersona -> onNavigateToSelectPersona(
                route.authorizedRequestInteractionId,
                route.dappDefinitionAddress
            )

            is InitialAuthorizedLoginRoute.OneTimeAccounts -> onNavigateToOneTimeAccounts(
                route.authorizedRequestInteractionId,
                route.isOneTimeRequest,
                route.isExactAccountsCount,
                route.numberOfAccounts,
                route.showBack
            )

            is InitialAuthorizedLoginRoute.OneTimePersonaData -> onNavigateToOneTimePersonaData(route.requiredPersonaFields)
            is InitialAuthorizedLoginRoute.OngoingPersonaData -> onNavigateToOngoingPersonaData(
                route.personaAddress,
                route.requiredPersonaFields
            )

            is InitialAuthorizedLoginRoute.OngoingAccounts -> onNavigateToOngoingAccounts(
                route.isOneTimeRequest,
                route.isExactAccountsCount,
                route.numberOfAccounts,
                route.showBack
            )

            is InitialAuthorizedLoginRoute.VerifyPersona -> onNavigateToVerifyPersona(
                route.walletAuthorizedRequestInteractionId,
                route.entitiesForProofWithSignatures
            )

            is InitialAuthorizedLoginRoute.VerifyAccounts -> onNavigateToVerifyAccounts(
                route.walletAuthorizedRequestInteractionId,
                route.entitiesForProofWithSignatures
            )

            else -> {}
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                backIconType = BackIconType.Close,
                onBackClick = viewModel::onAbortDappLogin,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RadixTheme.colors.background)
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
