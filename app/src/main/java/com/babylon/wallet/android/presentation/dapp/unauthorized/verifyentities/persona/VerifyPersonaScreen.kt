package com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.persona

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.Event
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.VerifyEntitiesContent
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.VerifyEntitiesViewModel
import kotlinx.collections.immutable.toImmutableList

@Composable
fun VerifyPersonaScreen(
    modifier: Modifier = Modifier,
    viewModel: VerifyEntitiesViewModel,
    sharedViewModel: DAppUnauthorizedLoginViewModel,
    onNavigateToVerifyAccounts: (String, EntitiesForProofWithSignatures) -> Unit,
    onVerificationFlowComplete: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()

    BackHandler {
        if (state.canNavigateBack) {
            onBackClick()
        } else {
            sharedViewModel.onUserRejectedRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is VerifyEntitiesViewModel.Event.NavigateToVerifyAccounts -> onNavigateToVerifyAccounts(
                    event.walletUnauthorizedRequestInteractionId,
                    event.entitiesForProofWithSignatures
                )
                VerifyEntitiesViewModel.Event.EntitiesVerified -> {
                    sharedViewModel.onRequestedEntitiesVerified(state.signatures)
                }
                VerifyEntitiesViewModel.Event.TerminateVerification -> {
                    sharedViewModel.onUserRejectedRequest()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.CloseLoginFlow -> onVerificationFlowComplete()
                Event.LoginFlowCompleted -> onVerificationFlowComplete()
                else -> {}
            }
        }
    }

    VerifyEntitiesContent(
        modifier = modifier,
        dapp = sharedState.dapp,
        entityType = state.entityType,
        profileEntities = state.nextEntitiesForProof.toImmutableList(),
        canNavigateBack = state.canNavigateBack,
        onContinueClick = viewModel::onContinueClick,
        isSigningInProgress = state.isSigningInProgress,
        onCloseClick = {
            if (state.canNavigateBack) {
                onBackClick()
            } else {
                sharedViewModel.onUserRejectedRequest()
            }
        }
    )
}
