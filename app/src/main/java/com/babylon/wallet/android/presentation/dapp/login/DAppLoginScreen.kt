package com.babylon.wallet.android.presentation.dapp.login

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MetadataConstants
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.BottomContinueButton
import com.babylon.wallet.android.presentation.ui.composables.PersonaCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.setSpanForPlaceholder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun DAppLoginScreen(
    viewModel: DAppLoginViewModel,
    onBackClick: () -> Unit,
    onHandleOngoingAccounts: (DAppLoginEvent.HandleOngoingAccounts) -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit,
    createNewPersona: () -> Unit,
    skipLoginScreen: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                DAppLoginEvent.RejectLogin -> onBackClick()
                is DAppLoginEvent.HandleOngoingAccounts -> onHandleOngoingAccounts(event)
                is DAppLoginEvent.LoginFlowCompleted -> onLoginFlowComplete(event.dappName)
                is DAppLoginEvent.ChooseAccounts -> onChooseAccounts(event)
                DAppLoginEvent.SkipSelectPersona -> skipLoginScreen()
            }
        }
    }
    val state by viewModel.state.collectAsState()
    BackHandler(enabled = true) {}
    DAppLoginContent(
        onCancelClick = viewModel::onRejectLogin,
        onLoginClick = viewModel::onLogin,
        onSelectPersona = viewModel::onSelectPersona,
        dappMetadata = state.dappMetadata,
        showProgress = state.showProgress,
        firstTimeLogin = state.firstTimeLogin,
        continueButtonEnabled = state.loginButtonEnabled,
        personas = state.personas,
        errorMessage = state.uiMessage,
        onMessageShown = viewModel::onMessageShown,
        createNewPersona = createNewPersona
    )
}

@Composable
private fun DAppLoginContent(
    onCancelClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSelectPersona: (String) -> Unit,
    dappMetadata: DappMetadata?,
    showProgress: Boolean,
    firstTimeLogin: Boolean,
    continueButtonEnabled: Boolean,
    personas: ImmutableList<PersonaUiModel>,
    createNewPersona: () -> Unit,
    errorMessage: UiMessage?,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
//            .systemBarsPadding()
            .navigationBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground)
    ) {
        AnimatedVisibility(
            visible = showProgress,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullscreenCircularProgressContent()
        }
        AnimatedVisibility(
            visible = !showProgress,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                model = dappMetadata?.getIcon(),
                                placeholder = painterResource(id = R.drawable.img_placeholder),
                                fallback = painterResource(id = R.drawable.img_placeholder),
                                error = painterResource(id = R.drawable.img_placeholder),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(104.dp)
                                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectDefault)
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
                                dappName = dappMetadata?.getName() ?: "Unknown dApp",
                                firstTimeLogin = firstTimeLogin,
                                modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
                            )
                            Text(
                                modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault),
                                text = stringResource(R.string.choose_a_persona),
                                textAlign = TextAlign.Center,
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.gray1
                            )
                        }
                        itemsIndexed(items = personas) { _, personaItem ->
                            PersonaCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectMedium)
                                    .clip(RadixTheme.shapes.roundedRectMedium)
                                    .throttleClickable {
                                        onSelectPersona(personaItem.persona.address)
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
                BottomContinueButton(
                    onLoginClick = onLoginClick,
                    loginButtonEnabled = continueButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RadixTheme.colors.defaultBackground)
                        .align(Alignment.BottomCenter)
                )
            }
            SnackbarUiMessageHandler(
                message = errorMessage,
                onMessageShown = onMessageShown,
                modifier = Modifier.imePadding()
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
fun DAppLoginContentPreview() {
    RadixWalletTheme {
        DAppLoginContent(
            onCancelClick = {},
            onLoginClick = {},
            onSelectPersona = {},
            dappMetadata = DappMetadata("address", mapOf(MetadataConstants.KEY_NAME to "Collabo.fi")),
            showProgress = false,
            firstTimeLogin = false,
            continueButtonEnabled = false,
            personas = persistentListOf(),
            createNewPersona = {},
            errorMessage = null,
            onMessageShown = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DAppLoginContentFirstTimePreview() {
    RadixWalletTheme {
        DAppLoginContent(
            onCancelClick = {},
            onLoginClick = {},
            onSelectPersona = {},
            dappMetadata = DappMetadata("address", mapOf(MetadataConstants.KEY_NAME to "Collabo.fi")),
            showProgress = false,
            firstTimeLogin = true,
            continueButtonEnabled = false,
            personas = persistentListOf(),
            createNewPersona = {},
            errorMessage = null,
            onMessageShown = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
