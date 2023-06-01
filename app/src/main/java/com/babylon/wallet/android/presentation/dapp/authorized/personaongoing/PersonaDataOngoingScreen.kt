package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.utils.formattedSpans

@Composable
fun PersonaDataOngoingScreen(
    viewModel: PersonaDataOngoingViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onEdit: (PersonaDataOngoingEvent.OnEditPersona) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (DAppAuthorizedLoginEvent.LoginFlowCompleted) -> Unit,
    onChooseAccounts: (DAppAuthorizedLoginEvent.ChooseAccounts) -> Unit,
    onPersonaDataOnetime: (DAppAuthorizedLoginEvent.PersonaDataOnetime) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
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
                is DAppAuthorizedLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event)
                is DAppAuthorizedLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppAuthorizedLoginEvent.PersonaDataOnetime -> onPersonaDataOnetime(event)
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
        dappWithMetadata = sharedState.dappWithMetadata,
        onBackClick = {
            if (sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData) {
                sharedViewModel.onAbortDappLogin()
            } else {
                onBackClick()
            }
        },
        isFirstScreenInFlow = sharedState.initialAuthorizedLoginRoute is InitialAuthorizedLoginRoute.OngoingPersonaData,
        persona = state.persona,
        onEditClick = viewModel::onEditClick,
        continueButtonEnabled = state.continueButtonEnabled
    )
}

@Composable
private fun PersonaDataOngoingPermissionContent(
    onContinueClick: () -> Unit,
    dappWithMetadata: DappWithMetadata?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirstScreenInFlow: Boolean,
    persona: PersonaUiModel?,
    onEditClick: (String) -> Unit,
    continueButtonEnabled: Boolean,
) {
    Column(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = if (isFirstScreenInFlow) Icons.Filled.Clear else Icons.Filled.ArrowBack,
                contentDescription = "clear"
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(RadixTheme.dimensions.paddingDefault)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            AsyncImage(
                model = rememberImageUrl(fromUrl = dappWithMetadata?.iconUrl?.toString(), size = ImageSize.MEDIUM),
                placeholder = painterResource(id = R.drawable.img_placeholder),
                fallback = painterResource(id = R.drawable.img_placeholder),
                error = painterResource(id = R.drawable.img_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectDefault)
                    .clip(RadixTheme.shapes.roundedRectDefault)
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
                dappName = dappWithMetadata?.name.orEmpty()
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
            Spacer(modifier = Modifier.weight(0.5f))
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Divider(color = RadixTheme.colors.gray5)
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            onClick = onContinueClick,
            enabled = continueButtonEnabled,
            text = stringResource(id = R.string.dAppRequest_personalDataPermission_continue)
        )
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

@Preview(showBackground = true)
@Composable
fun LoginPermissionContentPreview() {
    RadixWalletTheme {
        PersonaDataOngoingPermissionContent(
            onContinueClick = {},
            dappWithMetadata = DappWithMetadata(
                dAppAddress = "account_tdx_abc",
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            isFirstScreenInFlow = false,
            persona = PersonaUiModel(SampleDataProvider().samplePersona()),
            onEditClick = {},
            continueButtonEnabled = true
        )
    }
}
