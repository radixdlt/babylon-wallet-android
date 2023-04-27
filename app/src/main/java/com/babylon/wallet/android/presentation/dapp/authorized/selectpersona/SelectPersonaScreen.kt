package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.PersonaCard
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.setSpanForPlaceholder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun SelectPersonaScreen(
    viewModel: SelectPersonaViewModel,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    onBackClick: () -> Unit,
    onChooseAccounts: (DAppAuthorizedLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (DAppAuthorizedLoginEvent.LoginFlowCompleted) -> Unit,
    createNewPersona: (Boolean) -> Unit,
    onDisplayPermission: (DAppAuthorizedLoginEvent.DisplayPermission) -> Unit,
    onPersonaDataOngoing: (DAppAuthorizedLoginEvent.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (DAppAuthorizedLoginEvent.PersonaDataOnetime) -> Unit
) {
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                DAppAuthorizedLoginEvent.RejectLogin -> onBackClick()
                is DAppAuthorizedLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event)
                is DAppAuthorizedLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                is DAppAuthorizedLoginEvent.DisplayPermission -> onDisplayPermission(event)
                is DAppAuthorizedLoginEvent.PersonaDataOngoing -> onPersonaDataOngoing(event)
                is DAppAuthorizedLoginEvent.PersonaDataOnetime -> onPersonaDataOnetime(event)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is DAppSelectPersonaEvent.PersonaSelected -> sharedViewModel.onSelectPersona(event.persona)
                is DAppSelectPersonaEvent.CreatePersona -> createNewPersona(event.firstPersonaCreated)
            }
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = true) {}
    SelectPersonaContent(
        onCancelClick = sharedViewModel::onAbortDappLogin,
        onContinueClick = sharedViewModel::personaSelectionConfirmed,
        onSelectPersona = {
            sharedViewModel.onSelectPersona(it)
            viewModel.onSelectPersona(it.address)
        },
        dappMetadata = sharedState.dappMetadata,
        firstTimeLogin = state.firstTimeLogin,
        continueButtonEnabled = state.continueButtonEnabled,
        personas = state.personaListToDisplay,
        createNewPersona = viewModel::onCreatePersona,
        isLoading = state.isLoading
    )
}

@Composable
private fun SelectPersonaContent(
    onCancelClick: () -> Unit,
    onContinueClick: () -> Unit,
    onSelectPersona: (Network.Persona) -> Unit,
    dappMetadata: DappMetadata?,
    firstTimeLogin: Boolean,
    continueButtonEnabled: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    createNewPersona: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
) {
    Box(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isLoading,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FullscreenCircularProgressContent()
            }
            AnimatedVisibility(
                visible = !isLoading,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "clear"
                        )
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(RadixTheme.dimensions.paddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
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
                                    .size(104.dp)
                                    .background(
                                        RadixTheme.colors.gray3,
                                        RadixTheme.shapes.roundedRectDefault
                                    )
                                    .clip(RadixTheme.shapes.roundedRectDefault)
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                            Text(
                                text = stringResource(
                                    id = if (firstTimeLogin) {
                                        R.string.new_login_request
                                    } else {
                                        R.string.login_request
                                    }
                                ),
                                textAlign = TextAlign.Center,
                                style = RadixTheme.typography.title,
                                color = RadixTheme.colors.gray1
                            )
                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                            LoginRequestHeader(
                                dappName = dappMetadata?.name.orEmpty().ifEmpty { stringResource(id = R.string.unknown_dapp) },
                                firstTimeLogin = firstTimeLogin,
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
                            )
                            if (personas.isNotEmpty()) {
                                Text(
                                    modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                                    text = stringResource(R.string.choose_a_persona),
                                    textAlign = TextAlign.Center,
                                    style = RadixTheme.typography.body1Header,
                                    color = RadixTheme.colors.gray1
                                )
                            }
                        }
                        itemsIndexed(items = personas) { _, personaItem ->
                            PersonaCard(
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
                                text = stringResource(id = R.string.create_a_new_persona),
                                onClick = createNewPersona
                            )
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
            BottomPrimaryButton(
                onClick = onContinueClick,
                enabled = continueButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RadixTheme.colors.defaultBackground)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LoginRequestHeader(dappName: String, firstTimeLogin: Boolean, modifier: Modifier = Modifier) {
    val spanStyle = SpanStyle(color = RadixTheme.colors.gray1)
    val text = if (firstTimeLogin) {
        stringResource(
            R.string.dapp_login_first_time_subtitle,
            dappName
        ).setSpanForPlaceholder(dappName, spanStyle)
    } else {
        stringResource(
            R.string.dapp_login_subtitle,
            dappName
        ).setSpanForPlaceholder(dappName, spanStyle)
    }
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
            dappMetadata = DappMetadata(
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
            dappMetadata = DappMetadata(
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
