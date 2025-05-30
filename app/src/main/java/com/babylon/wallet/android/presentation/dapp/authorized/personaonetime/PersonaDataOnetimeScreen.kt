package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.displayName
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.DApp

@Composable
fun PersonaDataOnetimeScreen(
    viewModel: PersonaDataOnetimeViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()

    if (sharedState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            sharedViewModel.dismissNoMnemonicError()
        }
    }

    BackHandler {
        if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingAccounts) {
            sharedViewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.NavigateToVerifyPersona -> onNavigateToVerifyPersona(
                    event.walletUnauthorizedRequestInteractionId,
                    event.entitiesForProofWithSignatures
                )
                is Event.NavigateToVerifyAccounts -> onNavigateToVerifyAccounts(
                    event.walletUnauthorizedRequestInteractionId,
                    event.entitiesForProofWithSignatures
                )
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
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

    PersonaDataOnetimeContent(
        onContinueClick = {
            sharedViewModel.onGrantedOnetimePersonaData(state.selectedPersona())
        },
        dapp = sharedState.dapp,
        onBackClick = {
            if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OneTimePersonaData) {
                sharedViewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        showBack = state.showBack,
        personas = state.personaListToDisplay,
        onSelectPersona = viewModel::onSelectPersona,
        onCreatePersona = viewModel::onCreatePersona,
        onEditClick = viewModel::onEditClick,
        continueButtonEnabled = state.continueButtonEnabled,
        modifier = modifier
    )
}

@Composable
private fun PersonaDataOnetimeContent(
    onContinueClick: () -> Unit,
    dapp: DApp?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    onSelectPersona: ((Persona) -> Unit)?,
    onCreatePersona: () -> Unit,
    onEditClick: (Persona) -> Unit,
    continueButtonEnabled: Boolean,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.empty),
                onBackClick = onBackClick,
                backIconType = if (showBack) BackIconType.Back else BackIconType.Close,
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                enabled = continueButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                text = stringResource(id = R.string.dAppRequest_personalDataPermission_continue)
            )
        },
        containerColor = RadixTheme.colors.background
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
                    color = RadixTheme.colors.text
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                PermissionRequestHeader(
                    dappName = dapp.displayName()
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.dAppRequest_personalDataOneTime_chooseDataToProvide),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.header,
                    color = RadixTheme.colors.text
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
                            color = RadixTheme.colors.backgroundSecondary,
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
        .formattedSpans(
            boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.text)
        )
    Text(
        modifier = modifier,
        text = text,
        textAlign = TextAlign.Center,
        style = RadixTheme.typography.secondaryHeader,
        color = RadixTheme.colors.textSecondary
    )
}

@UsesSampleValues
@Preview
@Composable
private fun PersonaDataOnetimePreviewLight() {
    RadixWalletPreviewTheme {
        PersonaDataOnetimeContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            showBack = true,
            personas = persistentListOf(PersonaUiModel(Persona.sampleMainnet())),
            onSelectPersona = {},
            onCreatePersona = {},
            onEditClick = {},
            continueButtonEnabled = false
        )
    }
}

@UsesSampleValues
@Preview
@Composable
private fun PersonaDataOnetimePreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        PersonaDataOnetimeContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            showBack = true,
            personas = persistentListOf(PersonaUiModel(Persona.sampleMainnet())),
            onSelectPersona = {},
            onCreatePersona = {},
            onEditClick = {},
            continueButtonEnabled = false
        )
    }
}
