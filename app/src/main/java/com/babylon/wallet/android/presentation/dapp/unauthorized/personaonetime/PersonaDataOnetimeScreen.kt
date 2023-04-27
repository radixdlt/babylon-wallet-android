package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

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
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.persona.PersonaDetailCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.ImageSize
import com.babylon.wallet.android.utils.rememberImageUrl
import com.babylon.wallet.android.utils.setSpanForPlaceholder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun PersonaDataOnetimeScreen(
    viewModel: PersonaDataOnetimeViewModel,
    sharedViewModel: DAppUnauthorizedLoginViewModel,
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (requestId: String, dAppName: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppUnauthorizedLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event.requestId, event.dAppName)
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
        onContinueClick = sharedViewModel::onGrantedPersonaDataOnetime,
        dappMetadata = sharedState.dappMetadata,
        onBackClick = {
            if (sharedState.initialUnauthorizedLoginRoute is InitialUnauthorizedLoginRoute.OnetimePersonaData) {
                sharedViewModel.onRejectRequest()
            } else {
                onBackClick()
            }
        },
        isFirstScreenInFlow = sharedState.initialUnauthorizedLoginRoute is InitialUnauthorizedLoginRoute.OnetimePersonaData,
        personas = state.personaListToDisplay,
        onSelectPersona = {
            viewModel.onSelectPersona(it)
            sharedViewModel.onSelectPersona(it)
        },
        onCreatePersona = viewModel::onCreatePersona,
        onEditClick = viewModel::onEditClick,
        continueButtonEnabled = state.continueButtonEnabled
    )
}

@Composable
private fun PersonaDataOnetimeContent(
    onContinueClick: () -> Unit,
    dappMetadata: DappMetadata?,
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
                    model = rememberImageUrl(fromUrl = dappMetadata?.iconUrl?.toString(), size = ImageSize.MEDIUM),
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
                    text = stringResource(id = R.string.one_time_data_request),
                    textAlign = TextAlign.Center,
                    style = RadixTheme.typography.title,
                    color = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                PermissionRequestHeader(dappName = dappMetadata?.name.orEmpty().ifEmpty { stringResource(id = R.string.unknown_dapp) })
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = stringResource(id = R.string.choose_data_to_provide),
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
                    text = stringResource(id = R.string.create_a_new_persona),
                    onClick = onCreatePersona
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        Divider(color = RadixTheme.colors.gray5)
        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadixTheme.dimensions.paddingDefault),
            onClick = onContinueClick,
            text = stringResource(id = R.string.continue_button_title),
            enabled = continueButtonEnabled
        )
    }
}

@Composable
private fun PermissionRequestHeader(
    dappName: String,
    modifier: Modifier = Modifier
) {
    val spanStyle = SpanStyle(fontWeight = FontWeight.SemiBold, color = RadixTheme.colors.gray1)
    val oneTime = stringResource(id = R.string.just_one_time)
    val text = stringResource(id = R.string.dapp_is_requesting_onetime_persona_data_permission, dappName).setSpanForPlaceholder(
        dappName,
        spanStyle
    ).setSpanForPlaceholder(oneTime, spanStyle)
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
            dappMetadata = DappMetadata(
                nameItem = NameMetadataItem("Collabo.fi")
            ),
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            isFirstScreenInFlow = false,
            personas = persistentListOf(PersonaUiModel(SampleDataProvider().samplePersona())),
            onSelectPersona = {},
            onCreatePersona = {},
            onEditClick = {},
            continueButtonEnabled = true
        )
    }
}
