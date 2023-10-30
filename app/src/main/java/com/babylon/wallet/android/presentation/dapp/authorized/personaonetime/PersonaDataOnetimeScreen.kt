package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonaDataOnetimeScreen(
    viewModel: PersonaDataOnetimeViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
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
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.RequestCompletionBiometricPrompt -> {
                    if (event.requestDuringSigning) {
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
                else -> {}
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is PersonaDataOnetimeEvent.OnEditPersona -> {
                    onEdit(event)
                }

                is PersonaDataOnetimeEvent.CreatePersona -> onCreatePersona(event.firstPersonaCreated)
            }
        }
    }
    BackHandler {
        if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.Permission) {
            sharedViewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }
    PersonaDataOnetimeContent(
        onContinueClick = {
            sharedViewModel.onGrantedPersonaDataOnetime(state.selectedPersona())
        },
        dapp = sharedState.dapp,
        onBackClick = {
            if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OneTimePersonaData) {
                sharedViewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        isFirstScreenInFlow = sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OneTimePersonaData,
        personas = state.personaListToDisplay,
        onSelectPersona = viewModel::onSelectPersona,
        onCreatePersona = viewModel::onCreatePersona,
        onEditClick = viewModel::onEditClick,
        continueButtonEnabled = state.continueButtonEnabled,
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
private fun PersonaDataOnetimeContent(
    onContinueClick: () -> Unit,
    dapp: DApp?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirstScreenInFlow: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    onSelectPersona: ((Network.Persona) -> Unit)?,
    onCreatePersona: () -> Unit,
    onEditClick: (String) -> Unit,
    continueButtonEnabled: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (isFirstScreenInFlow) BackIconType.Close else BackIconType.Back,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            BottomPrimaryButton(
                onClick = onContinueClick,
                enabled = continueButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(RadixTheme.colors.defaultBackground),
                text = stringResource(id = R.string.dAppRequest_personalDataPermission_continue)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        LazyColumn(
            contentPadding = padding + PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Thumbnail.DApp(
                    modifier = Modifier.size(64.dp),
                    dapp = dapp
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                Text(
                    text = stringResource(id = R.string.dAppRequest_personalDataOneTime_title),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                PermissionRequestHeader(
                    dappName = dapp?.name.orEmpty()
                        .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) }
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.dAppRequest_personalDataOneTime_chooseDataToProvide),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.header,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            items(personas) { persona ->
                PersonaDetailCard(
                    persona = persona,
                    missingFields = persona.missingFieldKinds(),
                    onEditClick = onEditClick,
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectMedium)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .applyIf(
                            onSelectPersona != null,
                            Modifier.throttleClickable {
                                onSelectPersona?.invoke(persona.persona)
                            }
                        )
                        .fillMaxWidth()
                        .background(
                            color = RadixTheme.colors.gray5,
                            shape = RadixTheme.shapes.roundedRectMedium
                        ),
                    onSelectPersona = onSelectPersona
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            item {
                RadixSecondaryButton(
                    text = stringResource(id = R.string.createPersona_introduction_title),
                    onClick = onCreatePersona
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun PermissionRequestHeader(
    dappName: String,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_personalDataOneTime_subtitle, dappName)
        .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1))
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
fun LoginPermissionContentPreview() {
    RadixWalletTheme {
        PersonaDataOnetimeContent(
            onContinueClick = {},
            dapp = DApp(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            isFirstScreenInFlow = false,
            personas = persistentListOf(PersonaUiModel(SampleDataProvider().samplePersona())),
            onSelectPersona = {},
            onCreatePersona = {},
            onEditClick = {},
            continueButtonEnabled = false
        )
    }
}
