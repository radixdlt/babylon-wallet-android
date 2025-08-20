package com.babylon.wallet.android.presentation.nfc

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.none
import com.babylon.wallet.android.utils.findFragmentActivity
import com.radixdlt.sargon.ArculusCardFactorSource
import com.radixdlt.sargon.NfcTagArculusInteractonPurpose
import com.radixdlt.sargon.NfcTagDriverPurpose
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcDialog(
    viewModel: NfcViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity: Activity? = remember { context.findFragmentActivity() }

    BackHandler {
        viewModel.onDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.enableNfcReaderMode {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

            if (nfcAdapter?.isEnabled != true) {
                viewModel.onNfcDisabled()
                return@enableNfcReaderMode
            }

            nfcAdapter.enableReaderMode(
                activity,
                viewModel::onNfcTagDiscovered,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                Bundle().apply {
                    // Work around for some broken Nfc firmware implementations that poll the card too fast
                    putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
                }
            )
        }
    }

    NfcContent(
        modifier = modifier,
        state = state,
        onDismiss = viewModel::onDismiss,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onOpenNfcSettingsClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (ex: ActivityNotFoundException) {
                Timber.d(ex)
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                NfcViewModel.Event.Completed -> {
                    onDismiss()
                    viewModel.disableNfcReaderMode {
                        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                        nfcAdapter?.disableReaderMode(activity)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NfcContent(
    state: NfcViewModel.State,
    onDismiss: () -> Unit,
    onOpenNfcSettingsClick: () -> Unit,
    onDismissErrorMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val handleOnDismiss: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }
    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    DefaultModalSheetLayout(
        modifier = modifier,
        sheetState = sheetState,
        heightFraction = 0.6f,
        onDismissRequest = handleOnDismiss,
        sheetContent = {
            Scaffold(
                topBar = {
                    RadixCenteredTopAppBar(
                        windowInsets = WindowInsets.none,
                        title = "Ready to Scan",
                        onBackClick = handleOnDismiss,
                        backIconType = BackIconType.Close
                    )
                },
                bottomBar = {
                    RadixSecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        text = stringResource(id = R.string.common_cancel),
                        onClick = handleOnDismiss
                    )
                },
                containerColor = RadixTheme.colors.background
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    state.purpose?.let {
                        Text(
                            text = it.title(),
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text,
                            textAlign = TextAlign.Center
                        )
                    }

                    state.progress?.let {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXSmall))

                        Text(
                            text = "Progress: $it%", // TODO crowdin
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.text,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

                    Text(
                        text = "Hold your card flat against the back of the phone. Don’t move.\n" +
                            "This may take up to a minute.", // TODO crowdin
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingLarge,
                                vertical = RadixTheme.dimensions.paddingSmall
                            )
                            .padding(top = RadixTheme.dimensions.paddingDefault),
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = Icons.Outlined.Contactless,
                            contentDescription = null,
                            tint = RadixTheme.colors.icon
                        )
                    }
                }
            }
        }
    )

    if (state.errorMessage != null) {
        ErrorAlertDialog(
            cancel = onDismissErrorMessage,
            errorMessage = state.errorMessage,
            cancelMessage = stringResource(id = R.string.common_close)
        )
    }

    if (state.showNfcDisabled) {
        BasicPromptAlertDialog(
            titleText = "NFC is turned off", // TODO crowdin
            messageText = "Please enable NFC in system settings to continue.", // TODO crowdin
            confirmText = "Open Settings", // TODO crowdin
            finish = {
                if (it) {
                    onOpenNfcSettingsClick()
                }
                onDismiss()
            }
        )
    }
}

@Composable
private fun NfcTagDriverPurpose.title(): String {
    return when (this) {
        // TODO crowdin
        is NfcTagDriverPurpose.Arculus -> when (v1) {
            NfcTagArculusInteractonPurpose.ConfiguringCardMnemonic -> "Configuring your Arculus Card"
            is NfcTagArculusInteractonPurpose.ConfiguringCardPin -> "Configuring new Card PIN"
            is NfcTagArculusInteractonPurpose.DerivingPublicKeys -> "Updating Factor Config"
            NfcTagArculusInteractonPurpose.IdentifyingCard -> "Identifying Card"
            is NfcTagArculusInteractonPurpose.ProveOwnership -> "Signing Transaction"
            is NfcTagArculusInteractonPurpose.SignPreAuth -> "Signing Transaction"
            is NfcTagArculusInteractonPurpose.SignTransaction -> "Signing Transaction"
            is NfcTagArculusInteractonPurpose.VerifyingPin -> "Verifying Card PIN"
            is NfcTagArculusInteractonPurpose.RestoringCardPin -> "Restoring Card PIN"
        }
    }
}

@UsesSampleValues
@Composable
@Preview
private fun NfcPreview(
    @PreviewParameter(NfcPreviewProvider::class) state: NfcViewModel.State
) {
    RadixWalletPreviewTheme {
        NfcContent(
            state = state,
            onDismiss = {},
            onOpenNfcSettingsClick = {},
            onDismissErrorMessage = {}
        )
    }
}

@UsesSampleValues
class NfcPreviewProvider : PreviewParameterProvider<NfcViewModel.State> {

    override val values: Sequence<NfcViewModel.State>
        get() = sequenceOf(
            NfcViewModel.State(
                progress = "38%",
                purpose = NfcTagDriverPurpose.Arculus(
                    v1 = NfcTagArculusInteractonPurpose.DerivingPublicKeys(
                        v1 = ArculusCardFactorSource.sample()
                    )
                )
            ),
            NfcViewModel.State(
                errorMessage = UiMessage.ErrorMessage(Throwable("Error occurred"))
            ),
            NfcViewModel.State(
                showNfcDisabled = true
            )
        )
}
