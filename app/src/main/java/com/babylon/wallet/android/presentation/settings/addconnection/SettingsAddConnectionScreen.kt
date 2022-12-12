package com.babylon.wallet.android.presentation.settings.addconnection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun SettingsAddConnectionScreen(
    viewModel: SettingsAddConnectionViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsAddConnectionContent(
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .background(RadixTheme.colors.defaultBackground),
        hasAlreadyConnection = state.hasAlreadyConnection,
        onConnectionClick = viewModel::onConnectionClick,
        isLoading = state.isLoading,
        onBackClick = onBackClick
    )
}

@Composable
private fun SettingsAddConnectionContent(
    hasAlreadyConnection: Boolean,
    onConnectionClick: (String, String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    Column(modifier = modifier) {
        RadixCenteredTopAppBar(
            title = stringResource(R.string.new_connection),
            onBackClick = onBackClick,
            contentColor = RadixTheme.colors.gray1
        )
        if (hasAlreadyConnection) {
            ShowConnection()
        } else {
            if (isLoading) {
                FullscreenCircularProgressContent()
            } else {
                EnterConnection(
                    onConnectionClick = onConnectionClick
                )
            }
        }
    }
}

@Composable
private fun ShowConnection() {
    Text(
        text = "You have an active connection.",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun EnterConnection(
    modifier: Modifier = Modifier,
    onConnectionClick: (String, String) -> Unit
) {
    var connectionPasswordText by rememberSaveable { mutableStateOf("") }
    var connectionDisplayName by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier) {
        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            value = connectionPasswordText,
            onValueChanged = { connectionPasswordText = it },
            hint = stringResource(R.string.enter_the_connection_id)
        )

        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

        RadixTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            value = connectionDisplayName,
            onValueChanged = { connectionDisplayName = it },
            hint = stringResource(R.string.enter_the_display_name)
        )

        Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

        RadixPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            text = stringResource(id = R.string.add_connection),
            onClick = {
                onConnectionClick(connectionPasswordText, connectionDisplayName)
            }
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithoutActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsAddConnectionContent(
            hasAlreadyConnection = false,
            onConnectionClick = { _, _ -> },
            isLoading = false,
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenAddConnectionWithActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsAddConnectionContent(
            hasAlreadyConnection = true,
            onConnectionClick = { _, _ -> },
            isLoading = false,
            onBackClick = {}
        )
    }
}
