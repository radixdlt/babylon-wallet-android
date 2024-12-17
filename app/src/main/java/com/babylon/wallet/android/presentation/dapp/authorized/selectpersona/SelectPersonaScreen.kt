package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.SelectPersonaViewModel.Event.CreatePersona
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.card.PersonaSelectableCard
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import rdx.works.core.domain.DApp

@Composable
fun SelectPersonaScreen(
    viewModel: SelectPersonaViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    onChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onNavigateToOngoingAccounts: (Event.NavigateToOngoingAccounts) -> Unit,
    onNavigateToOngoingPersonaData: (Event.NavigateToOngoingPersonaData) -> Unit,
    onNavigateToOneTimePersonaData: (Event.NavigateToOneTimePersonaData) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()

    if (sharedState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            sharedViewModel.dismissNoMnemonicError()
        }
    }

    HandleOneOffEvents(
        oneOffEvent = sharedViewModel.oneOffEvent,
        onBackClick = onBackClick,
        onLoginFlowComplete = onLoginFlowComplete,
        onChooseAccounts = onChooseAccounts,
        onNavigateToOngoingAccounts = onNavigateToOngoingAccounts,
        onNavigateToOngoingPersonaData = onNavigateToOngoingPersonaData,
        onNavigateToOneTimePersonaData = onNavigateToOneTimePersonaData
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is CreatePersona -> createNewPersona(event.firstPersonaCreated)
                SelectPersonaViewModel.Event.TerminateFlow -> sharedViewModel.onAbortDappLogin()
                is SelectPersonaViewModel.Event.PersonaAuthorized -> sharedViewModel.onPersonaAuthorized(
                    authorizedPersona = event.persona,
                    signature = event.signature
                )
                is SelectPersonaViewModel.Event.AuthorizationFailed -> sharedViewModel.handleRequestError(exception = event.throwable)
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
        modifier = modifier,
        onCancelClick = sharedViewModel::onAbortDappLogin,
        onContinueClick = viewModel::onContinueClick,
        onPersonaSelected = { viewModel.onPersonaSelected(it.address) },
        createNewPersona = viewModel::onCreatePersona,
        dapp = sharedState.dapp,
        state = state
    )
}

@Composable
private fun HandleOneOffEvents(
    oneOffEvent: Flow<Event>,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    onNavigateToOngoingAccounts: (Event.NavigateToOngoingAccounts) -> Unit,
    onNavigateToOngoingPersonaData: (Event.NavigateToOngoingPersonaData) -> Unit,
    onNavigateToOneTimePersonaData: (Event.NavigateToOneTimePersonaData) -> Unit
) {
    LaunchedEffect(Unit) {
        oneOffEvent.collect { event ->
            when (event) {
                Event.CloseLoginFlow -> onBackClick()
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.NavigateToChooseAccounts -> onChooseAccounts(event)
                is Event.NavigateToOngoingAccounts -> onNavigateToOngoingAccounts(event)
                is Event.NavigateToOngoingPersonaData -> onNavigateToOngoingPersonaData(event)
                is Event.NavigateToOneTimePersonaData -> onNavigateToOneTimePersonaData(event)
                else -> {} // no verify entities in the login request
            }
        }
    }
}

@Composable
private fun SelectPersonaContent(
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    onPersonaSelected: (Persona) -> Unit,
    createNewPersona: () -> Unit,
    dapp: DApp?,
    state: SelectPersonaViewModel.State,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                backIconType = BackIconType.Close,
                onBackClick = onCancelClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                enabled = state.isContinueButtonEnabled,
                text = stringResource(id = R.string.dAppRequest_login_continue),
                isLoading = state.isSigningInProgress
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        AnimatedVisibility(
            visible = state.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FullscreenCircularProgressContent()
        }

        AnimatedVisibility(
            visible = !state.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = RadixTheme.dimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Thumbnail.DApp(
                        modifier = Modifier.size(64.dp),
                        dapp = dapp
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                    Text(
                        text = stringResource(
                            id = if (state.isFirstTimeLogin) {
                                R.string.dAppRequest_login_titleNewDapp
                            } else {
                                R.string.dAppRequest_login_titleKnownDapp
                            }
                        ),
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    LoginRequestHeader(
                        dappName = dapp.displayName(),
                        firstTimeLogin = state.isFirstTimeLogin,
                        modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge)
                    )
                    if (state.personas.isNotEmpty()) {
                        Text(
                            modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge),
                            text = stringResource(R.string.dAppRequest_login_choosePersona),
                            textAlign = TextAlign.Center,
                            style = RadixTheme.typography.body1Header,
                            color = RadixTheme.colors.gray1
                        )
                    }
                }
                itemsIndexed(items = state.personas) { index, personaItem ->
                    val addSpacer = index != state.personas.lastIndex
                    PersonaSelectableCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                RadixTheme.colors.gray5,
                                shape = RadixTheme.shapes.roundedRectMedium
                            )
                            .clip(RadixTheme.shapes.roundedRectMedium)
                            .throttleClickable {
                                onPersonaSelected(personaItem.persona)
                            },
                        persona = personaItem,
                        onSelectPersona = onPersonaSelected
                    )
                    if (addSpacer) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                    }
                }
                item {
                    RadixSecondaryButton(
                        modifier = Modifier.padding(top = RadixTheme.dimensions.paddingLarge),
                        text = stringResource(id = R.string.personas_createNewPersona),
                        onClick = createNewPersona
                    )
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SelectPersonaPreview() {
    RadixWalletTheme {
        SelectPersonaContent(
            onCancelClick = {},
            onContinueClick = {},
            onPersonaSelected = {},
            createNewPersona = {},
            dapp = DApp.sampleMainnet(),
            state = SelectPersonaViewModel.State(
                isLoading = false,
                authorizedDApp = AuthorizedDapp.sampleMainnet(),
                personas = persistentListOf(Persona.sampleMainnet().toUiModel())
            )
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun SelectPersonaFirstTimePreview() {
    RadixWalletTheme {
        SelectPersonaContent(
            onCancelClick = {},
            onContinueClick = {},
            onPersonaSelected = {},
            createNewPersona = {},
            dapp = DApp.sampleMainnet(),
            state = SelectPersonaViewModel.State(
                isLoading = false,
                authorizedDApp = null,
                personas = persistentListOf()
            )
        )
    }
}
