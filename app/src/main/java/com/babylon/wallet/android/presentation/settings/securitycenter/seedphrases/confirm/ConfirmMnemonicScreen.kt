package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.confirm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.SeedPhraseInputVerificationForm
import com.babylon.wallet.android.presentation.ui.composables.BottomPrimaryButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SecureScreen
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.sargon.sample

@Composable
fun ConfirmMnemonicScreen(
    viewModel: ConfirmMnemonicViewModel,
    onMnemonicBackedUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    ConfirmMnemonicContent(
        state = state,
        onBackClick = onDismiss,
        onSubmitClick = {
            context.biometricAuthenticate { result ->
                if (result == BiometricAuthenticationResult.Succeeded) {
                    viewModel.onSubmit()
                }
            }
        },
        onWordTyped = viewModel::onWordChanged,
        onMessageShown = viewModel::onMessageShown
    )

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                ConfirmMnemonicViewModel.Event.MoveToNextWord -> focusManager.moveFocus(FocusDirection.Next)
                ConfirmMnemonicViewModel.Event.MnemonicBackedUp -> onMnemonicBackedUp()
            }
        }
    }
}

@Composable
private fun ConfirmMnemonicContent(
    modifier: Modifier = Modifier,
    state: ConfirmMnemonicViewModel.State,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onWordTyped: (Int, String) -> Unit,
    onMessageShown: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    var focusedWordIndex by remember {
        mutableStateOf<Int?>(null)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        bottomBar = {
            BottomPrimaryButton(
                text = stringResource(id = R.string.common_continue),
                onClick = onSubmitClick,
                enabled = state.seedPhraseState.allFieldsHaveValue,
                insets = WindowInsets.navigationBars.union(WindowInsets.ime)
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.defaultBackground
    ) { padding ->
        SeedPhraseView(
            modifier = Modifier.padding(padding),
            state = state,
            onWordChanged = onWordTyped,
            onFocusedWordIndexChanged = { focusedWordIndex = it }
        )
    }
}

@Composable
private fun SeedPhraseView(
    modifier: Modifier = Modifier,
    state: ConfirmMnemonicViewModel.State,
    onWordChanged: (Int, String) -> Unit,
    onFocusedWordIndexChanged: (Int) -> Unit,
) {
    SecureScreen()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            text = stringResource(id = R.string.confirmMnemonicBackedUp_title),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.title
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = stringResource(id = R.string.confirmMnemonicBackedUp_subtitle),
            textAlign = TextAlign.Center,
            style = RadixTheme.typography.body1Header
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

        SeedPhraseInputVerificationForm(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
            seedPhraseWords = state.seedPhraseState.seedPhraseWords,
            onWordChanged = onWordChanged,
            onFocusedWordIndexChanged = onFocusedWordIndexChanged
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun ConfirmMnemonicContentPreview() {
    RadixWalletTheme {
        ConfirmMnemonicContent(
            state = ConfirmMnemonicViewModel.State(
                factorSource = FactorSource.Device.sample()
            ),
            onBackClick = {},
            onSubmitClick = {},
            onWordTyped = { _, _ -> },
            onMessageShown = {}
        )
    }
}
