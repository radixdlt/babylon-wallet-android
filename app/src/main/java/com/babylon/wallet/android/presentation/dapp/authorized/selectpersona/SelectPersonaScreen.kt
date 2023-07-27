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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalContext
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
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.status.signing.SigningStatusBottomDialog
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.PersonaCard
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.biometricAuthenticateSuspend
import com.babylon.wallet.android.utils.formattedSpans
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
    LaunchedEffect(Unit) {
        sharedViewModel.oneOffEvent.collect { event ->
            when (event) {
                Event.RejectLogin -> onBackClick()
                is Event.LoginFlowCompleted -> onLoginFlowComplete()
                is Event.ChooseAccounts -> onChooseAccounts(event)
                is Event.DisplayPermission -> onDisplayPermission(event)
                is Event.PersonaDataOngoing -> onPersonaDataOngoing(event)
                is Event.PersonaDataOnetime -> onPersonaDataOnetime(event)
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
                                model = rememberImageUrl(fromUrl = dappWithMetadata?.iconUrl?.toString(), size = ImageSize.MEDIUM),
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
                                text = stringResource(id = R.string.personas_createNewPersona),
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
                    .align(Alignment.BottomCenter),
                text = stringResource(id = R.string.dAppRequest_login_continue)
            )
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
