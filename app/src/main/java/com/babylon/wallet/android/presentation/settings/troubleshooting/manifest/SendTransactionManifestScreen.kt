package com.babylon.wallet.android.presentation.settings.troubleshooting.manifest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextFieldDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.copyToClipboard

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
        onCopyClick = {
            context.copyToClipboard(
                label = "Transaction Manifest",
                value = state.manifest
            )
        },
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
    onCopyClick: () -> Unit,
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
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
            ) {
                Button(
                    text = "Paste",
                    iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_add_override,
                    onClick = onPasteClick
                )

                Button(
                    text = "Clear",
                    iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_close,
                    onClick = onClearClick,
                    enabled = state.isManifestNotBlank
                )

                Button(
                    text = stringResource(R.string.common_copy),
                    iconRes = R.drawable.ic_copy,
                    onClick = onCopyClick
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
}

@Composable
private fun Button(
    text: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier
            .height(40.dp),
        onClick = onClick,
        shape = RadixTheme.shapes.roundedRectSmall,
        elevation = null,
        colors = ButtonColors(
            containerColor = RadixTheme.colors.backgroundTertiary,
            contentColor = RadixTheme.colors.icon,
            disabledContainerColor = RadixTheme.colors.backgroundTertiary,
            disabledContentColor = RadixTheme.colors.textTertiary
        ),
        enabled = enabled
    ) {
        iconRes?.let {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = it),
                contentDescription = "copy"
            )
        }

        Text(
            modifier = Modifier.padding(start = RadixTheme.dimensions.paddingXXSmall),
            text = text,
            style = RadixTheme.typography.body1Header
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SendTransactionManifestContentPreview(
    @PreviewParameter(SendTransactionManifestPreviewProvider::class) state: SendTransactionManifestViewModel.State
) {
    RadixWalletTheme {
        SendTransactionManifestContent(
            modifier = Modifier,
            state = state,
            onBackClick = {},
            onPreviewClick = {},
            onManifestChanged = {},
            onCopyClick = {},
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
                manifest = ""
            ),
            SendTransactionManifestViewModel.State(
                manifest = "CALL_METHOD\n" +
                    "    Address(\"account_tdx_2_129rq58xf8tzuu4zn0v705plfwm7xpdfv4ch2a4uvxalj0wzp45smye\")\n" +
                    "    \"withdraw\"\n" +
                    "    Address(\"resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc\")\n" +
                    "    Decimal(\"123\")\n" +
                    ";\n" +
                    "TAKE_FROM_WORKTOP\n" +
                    "    Address(\"resource_tdx_2_1tknxxxxxxxxxradxrdxxxxxxxxx009923554798xxxxxxxxxtfd2jc\")\n" +
                    "    Decimal(\"123\")\n" +
                    "    Bucket(\"bucket1\")\n" +
                    ";\n" +
                    "CALL_METHOD\n" +
                    "    Address(\"account_tdx_2_129u84q4u37e3xsqxpe33stpwhuh7jh7t32pu0l4qe89ux2axtud3gs\")\n" +
                    "    \"try_deposit_or_abort\"\n" +
                    "    Bucket(\"bucket1\")\n" +
                    "    Enum<0u8>()\n" +
                    ";"
            )
        )
}
