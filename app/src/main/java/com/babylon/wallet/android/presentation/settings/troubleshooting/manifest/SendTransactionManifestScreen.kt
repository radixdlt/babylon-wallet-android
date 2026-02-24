package com.babylon.wallet.android.presentation.settings.troubleshooting.manifest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextFieldDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner

@Composable
fun SendTransactionManifestScreen(
    viewModel: SendTransactionManifestViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = context.getSystemService<android.content.ClipboardManager>()

    SendTransactionManifestContent(
        state = state,
        onBackClick = onBackClick,
        onPreviewClick = viewModel::onPreviewClick,
        onManifestChanged = viewModel::onManifestChanged,
        onPasteClick = {
            // Safely read the latest clipboard item
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(context).toString()
                viewModel.onManifestChanged(text)
            }
        },
        onClearClick = viewModel::onClearClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage
    )
}

@Composable
private fun SendTransactionManifestContent(
    state: SendTransactionManifestViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onManifestChanged: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onDismissErrorMessage: () -> Unit
) {
    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissErrorMessage
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RadixCenteredTopAppBar(
                title = "Submit Transaction Manifest",
                onBackClick = onBackClick,
                windowInsets = WindowInsets.statusBarsAndBanner
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        containerColor = RadixTheme.colors.backgroundSecondary,
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingDefault
                            ),
                        text = "Transaction Preview",
                        onClick = onPreviewClick,
                        enabled = state.isManifestNotBlank
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = "Enter a raw transaction manifest to preview and submit to the network.",
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Row(
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                if (state.lineCount > 0) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                        text = "${state.lineCount} ${if (state.lineCount == 1) "line" else "lines"}",
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.textSecondary
                    )
                }

                RadixSecondaryButton(
                    text = "Paste",
                    onClick = onPasteClick
                )

                RadixSecondaryButton(
                    text = "Clear",
                    onClick = onClearClick,
                    enabled = state.isManifestNotBlank
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxSize(),
                value = state.manifest,
                onValueChange = onManifestChanged,
                shape = RadixTheme.shapes.roundedRectSmall,
                colors = RadixTextFieldDefaults.colors(),
                placeholder = {
                    Text(
                        text = "Raw Transaction Manifest",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = RadixTheme.colors.textSecondary
                        )
                    )
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = RadixTheme.colors.text
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SendTransactionManifestContentPreview(
    @PreviewParameter(SendTransactionManifestPreviewProvider::class) state: SendTransactionManifestViewModel.State
) {
    RadixWalletTheme {
        SendTransactionManifestContent(
            modifier = Modifier,
            state = state,
            onBackClick = {},
            onPreviewClick = {},
            onManifestChanged = {},
            onPasteClick = {},
            onClearClick = {},
            onDismissErrorMessage = {}
        )
    }
}

class SendTransactionManifestPreviewProvider : PreviewParameterProvider<SendTransactionManifestViewModel.State> {

    override val values: Sequence<SendTransactionManifestViewModel.State>
        get() = sequenceOf(
            SendTransactionManifestViewModel.State(
                isLoading = false,
                manifest = ""
            )
        )
}