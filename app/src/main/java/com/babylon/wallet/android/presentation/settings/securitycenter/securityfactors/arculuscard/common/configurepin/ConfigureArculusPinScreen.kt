package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.configurepin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.PinTextField
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.WarningText
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.keyboardVisiblePadding
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH
import kotlinx.coroutines.delay

@Composable
fun SetArculusPinScreen(
    modifier: Modifier = Modifier,
    state: ConfigureArculusPinState,
    topBarTitle: String,
    contentTitle: (@Composable () -> Unit)?,
    description: String,
    pinInputTitle: String,
    confirmPinInputTitle: String,
    confirmButtonTitle: String,
    onDismiss: () -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onConfirmClick: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val pinFocusRequester = remember { FocusRequester() }
    val confirmPinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        pinFocusRequester.requestFocus()
    }

    LaunchedEffect(state.isConfirmedPinEnabled) {
        if (state.isConfirmedPinEnabled) {
            confirmPinFocusRequester.requestFocus()
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.infoMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onDismissMessage
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = topBarTitle,
                onBackClick = onDismiss,
                backIconType = BackIconType.Back,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        containerColor = RadixTheme.colors.background,
        bottomBar = {
            RadixBottomBar(
                onClick = onConfirmClick,
                text = confirmButtonTitle,
                enabled = state.isConfirmButtonEnabled,
                isLoading = state.isConfirmButtonLoading
            )
        },
        snackbarHost = {
            RadixSnackbarHost(
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                hostState = snackBarHostState
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .keyboardVisiblePadding(
                    padding = padding,
                    bottom = RadixTheme.dimensions.paddingDefault
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RadixTheme.dimensions.paddingSemiLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            contentTitle?.let {
                it()

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            }

            Text(
                text = description,
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXXLarge))

            Column {
                Text(
                    text = pinInputTitle,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                PinTextField(
                    modifier = Modifier.focusRequester(pinFocusRequester),
                    pinValue = state.pin,
                    pinLength = ARCULUS_PIN_LENGTH,
                    onPinChange = onPinChange,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                Text(
                    text = confirmPinInputTitle,
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.text
                )

                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                val focusManager = LocalFocusManager.current

                PinTextField(
                    modifier = Modifier.focusRequester(confirmPinFocusRequester),
                    pinValue = state.confirmedPin,
                    pinLength = ARCULUS_PIN_LENGTH,
                    onPinChange = onConfirmPinChange,
                    imeAction = ImeAction.Done,
                    isEnabled = state.isConfirmedPinEnabled,
                    onPinComplete = { focusManager.clearFocus() },
                )

                if (state.showPinsNotMatchingError) {
                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    WarningText(
                        text = AnnotatedString("PINs do not match"),
                        textStyle = RadixTheme.typography.body2HighImportance,
                        contentColor = RadixTheme.colors.error
                    )
                }
            }
        }
    }

    state.errorMessage?.let { error ->
        ErrorAlertDialog(
            cancel = onDismissMessage,
            errorMessage = error,
            cancelMessage = stringResource(id = R.string.common_close)
        )
    }
}
