package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaSelectableCard
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun SelectPersonaScreen(
    viewModel: SelectPersonaViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onDisplayPermission: (Event.DisplayPermission) -> Unit,
    onPersonaDataOngoing: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    if (sharedState.isNoMnemonicErrorVisible) {
        BasicPromptAlertDialog(
            finish = {
                sharedViewModel.dismissNoMnemonicError()
            },
            title = stringResource(id = R.string.transactionReview_noMnemonicError_title),
            text = stringResource(id = R.string.transactionReview_noMnemonicError_text),
            dismissText = null
        )
    }
    HandleOneOffEvents(
        oneOffEvent = sharedViewModel.oneOffEvent,
        onBackClick = onBackClick,
        onLoginFlowComplete = onLoginFlowComplete,
        onChooseAccounts = onChooseAccounts,
        onDisplayPermission = onDisplayPermission,
        onPersonaDataOngoing = onPersonaDataOngoing,
        onPersonaDataOnetime = onPersonaDataOnetime,
        onBiometricPrompt = { signatureRequired ->
            if (signatureRequired) {
                sharedViewModel.completeRequestHandling(deviceBiometricAuthenticationProvider = {
                    context.biometricAuthenticateSuspend()
                })
            } else {
                context.biometricAuthenticate { authenticated ->
                    if (authenticated) {
                        sharedViewModel.completeRequestHandling()
                    }
                }
            }
        }
    )
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppSelectPersonaEvent.PersonaSelected -> sharedViewModel.onSelectPersona(event.persona)
                is DAppSelectPersonaEvent.CreatePersona -> createNewPersona(event.firstPersonaCreated)
            }
        }
    }
    BackHandler {
        if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.SelectPersona) {
            sharedViewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }
    SelectPersonaContent(
        onCancelClick = sharedViewModel::onAbortDappLogin,
        onContinueClick = sharedViewModel::personaSelectionConfirmed,
        onSelectPersona = {
            sharedViewModel.onSelectPersona(it)
            viewModel.onSelectPersona(it.address)
        },
        dappWithMetadata = sharedState.dappWithMetadata,
        firstTimeLogin = state.firstTimeLogin,
        continueButtonEnabled = state.continueButtonEnabled,
        personas = state.personaListToDisplay,
        createNewPersona = viewModel::onCreatePersona,
        isLoading = state.isLoading,
        modifier = modifier
    )
    sharedState.interactionState?.let {
        SigningStatusBottomDialog(
            modifier = Modifier.fillMaxHeight(0.8f),
            onDismissDialogClick = sharedViewModel::onDismissSigningStatusDialog,
            interactionState = it
        )
    }
}

@Composable
private fun HandleOneOffEvents(
    oneOffEvent: Flow<Event>,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onDisplayPermission: (Event.DisplayPermission) -> Unit,
    onPersonaDataOngoing: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    onBiometricPrompt: (signtureReguired: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                Event.CloseLoginFlow -> onBackClick()
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.DisplayPermission -> onDisplayPermission(event)
                is Event.PersonaDataOngoing -> onPersonaDataOngoing(event)
                is Event.PersonaDataOnetime -> onPersonaDataOnetime(event)
                is Event.RequestCompletionBiometricPrompt -> onBiometricPrompt(event.isSignatureRequired)
            }
        }
    }
}

@Composable
private fun SelectPersonaContent(
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    onSelectPersona: (Network.Persona) -> Unit,
    dappWithMetadata: DAppWithMetadata?,
    firstTimeLogin: Boolean,
    continueButtonEnabled: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    createNewPersona: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                backIconType = BackIconType.Close,
                onBackClick = onCancelClick,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            BottomPrimaryButton(
                onClick = onContinueClick,
                enabled = continueButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                text = stringResource(id = R.string.dAppRequest_login_continue)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        AnimatedVisibility(
            visible = isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FullscreenCircularProgressContent()
        }

        AnimatedVisibility(
            visible = !isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Thumbnail.DApp(
                        modifier = Modifier.size(104.dp),
                        dapp = dappWithMetadata
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        text = stringResource(
                            id = if (firstTimeLogin) {
                                R.string.dAppRequest_login_titleNewDapp
                            } else {
                                R.string.dAppRequest_login_titleKnownDapp
                            }
                        ),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    LoginRequestHeader(
                        dappName = dappWithMetadata?.name.orEmpty().ifEmpty {
                            stringResource(
                                id = R.string.dAppRequest_metadata_unknownName
                            )
                        },
                        firstTimeLogin = firstTimeLogin,
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
                    )
                    if (personas.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                            text = stringResource(R.string.dAppRequest_login_choosePersona),
                            textAlign = TextAlign.Center,
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
                itemsIndexed(items = personas) { _, personaItem ->
                    PersonaSelectableCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.gray5,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onSelectPersona(personaItem.persona)
                            },
                        persona = personaItem,
                        onSelectPersona = onSelectPersona
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
                item {
                    RadixSecondaryButton(
                        text = stringResource(id = R.string.personas_createNewPersona),
                        onClick = createNewPersona
                    )
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun LoginRequestHeader(dappName: String, firstTimeLogin: Boolean, modifier: Modifier = Modifier) {
    val text = if (firstTimeLogin) {
        stringResource(
            R.string.dAppRequest_login_subtitleNewDapp,
            dappName
        )
    } else {
        stringResource(
            R.string.dAppRequest_login_subtitleKnownDapp,
            dappName
        )
    }.formattedSpans(boldStyle = SpanStyle(color = RadixTheme.colors.gray1))
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.gray2
    )
}

@Preview(showBackground = true)
@Composable
fun SelectPersonaPreview() {
    RadixWalletTheme {
        SelectPersonaContent(
            onCancelClick = {},
            onContinueClick = {},
            onSelectPersona = {},
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            firstTimeLogin = false,
            continueButtonEnabled = false,
            personas = persistentListOf(),
            createNewPersona = {},
            modifier = Modifier.fillMaxSize(),
            isLoading = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SelectPersonaFirstTimePreview() {
    RadixWalletTheme {
        SelectPersonaContent(
            onCancelClick = {},
            onContinueClick = {},
            onSelectPersona = {},
            dappWithMetadata = DAppWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            firstTimeLogin = true,
            continueButtonEnabled = false,
            personas = persistentListOf(),
            createNewPersona = {},
            modifier = Modifier.fillMaxSize(),
            isLoading = false
        )
    }
}
