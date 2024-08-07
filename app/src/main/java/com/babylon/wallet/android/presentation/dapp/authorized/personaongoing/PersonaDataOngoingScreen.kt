package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NoMnemonicAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.formattedSpans
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.DApp

@Suppress("CyclomaticComplexMethod")
@Composable
fun PersonaDataOngoingScreen(
    viewModel: PersonaDataOngoingViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onEdit: (PersonaDataOngoingEvent.OnEditPersona) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    if (sharedState.isNoMnemonicErrorVisible) {
        NoMnemonicAlertDialog {
            sharedViewModel.dismissNoMnemonicError()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is PersonaDataOngoingEvent.OnEditPersona -> {
                    onEdit(event)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.PersonaDataOnetime -> onPersonaDataOnetime(event)
                is Event.CloseLoginFlow -> onLoginFlowComplete()
                else -> {}
            }
        }
    }
    BackHandler {
        if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData) {
            sharedViewModel.onAbortDappLogin()
        } else {
            onBackClick()
        }
    }
    PersonaDataOngoingPermissionContent(
        onContinueClick = sharedViewModel::onGrantedPersonaDataOngoing,
        dapp = sharedState.dapp,
        onBackClick = {
            if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData) {
                sharedViewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        showBack = state.showBack,
        persona = state.persona,
        onEditClick = viewModel::onEditClick,
        continueButtonEnabled = state.continueButtonEnabled,
        modifier = modifier
    )
}

@Composable
private fun PersonaDataOngoingPermissionContent(
    onContinueClick: () -> Unit,
    dapp: DApp?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean,
    persona: PersonaUiModel?,
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
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            RadixBottomBar(
                onClick = onContinueClick,
                enabled = continueButtonEnabled,
                text = stringResource(id = R.string.dAppRequest_personalDataPermission_continue)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Thumbnail.DApp(
                modifier = Modifier.size(64.dp),
                dapp = dapp
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = stringResource(id = R.string.dAppRequest_personalDataPermission_title),
                textAlign = TextAlign.Center,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray1
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            PermissionRequestHeader(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingLarge),
                dappName = dapp?.name.orEmpty()
                    .ifEmpty { stringResource(id = R.string.dAppRequest_metadata_unknownName) }
            )
            persona?.let {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                PersonaDetailCard(
                    persona = persona,
                    missingFields = persona.missingFieldKinds(),
                    onEditClick = onEditClick,
                    modifier = Modifier
                        .shadow(elevation = 4.dp, shape = RadixTheme.shapes.roundedRectMedium)
                        .clip(RadixTheme.shapes.roundedRectMedium)
                        .fillMaxWidth()
                        .background(
                            color = RadixTheme.colors.gray5,
                            shape = RadixTheme.shapes.roundedRectMedium
                        )
                )
            }
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                text = stringResource(R.string.dAppRequest_personalDataPermission_updateInSettingsExplanation),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        }
    }
}

@Composable
private fun PermissionRequestHeader(
    dappName: String,
    modifier: Modifier = Modifier
) {
    val text = stringResource(id = R.string.dAppRequest_personalDataPermission_subtitle, dappName)
        .formattedSpans(boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1))
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
fun LoginPermissionContentPreview() {
    RadixWalletTheme {
        PersonaDataOngoingPermissionContent(
            onContinueClick = {},
            dapp = DApp.sampleMainnet(),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            showBack = true,
            persona = PersonaUiModel(Persona.sampleMainnet()),
            onEditClick = {},
            continueButtonEnabled = true
        )
    }
}
