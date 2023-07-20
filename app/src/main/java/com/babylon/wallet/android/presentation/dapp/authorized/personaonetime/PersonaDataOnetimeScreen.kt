package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.SigningStateDialog
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
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
    onLoginFlowComplete: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                Event.RequestCompletionBiometricPrompt -> {
                    context.biometricAuthenticate { authenticated ->
                        if (authenticated) {
                            sharedViewModel.sendRequestResponse()
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
        dappWithMetadata = sharedState.dappWithMetadata,
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
        continueButtonEnabled = state.continueButtonEnabled
    )
    SigningStateDialog(sharedState.factorSourceInteractionState)
}

@Composable
private fun PersonaDataOnetimeContent(
    onContinueClick: () -> Unit,
    dappWithMetadata: DAppWithMetadata?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirstScreenInFlow: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    onSelectPersona: ((Network.Persona) -> Unit)?,
    onCreatePersona: () -> Unit,
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
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(RadixTheme.dimensions.paddingDefault),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
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
                    text = stringResource(id = R.string.dAppRequest_personalDataOneTime_title),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                PermissionRequestHeader(
                    dappName = dappWithMetadata?.name.orEmpty()
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
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Divider(color = RadixTheme.colors.gray5)
        RadixPrimaryButton(
            text = stringResource(
                id = R.string.dAppRequest_personalDataOneTime_continue
            ),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            enabled = continueButtonEnabled
        )
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
            dappWithMetadata = DAppWithMetadata(
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
