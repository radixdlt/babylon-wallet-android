package com.babylon.wallet.android.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {

    val state = viewModel.settingsState
    SettingsContent(
        isConnectionOpen = state.isConnectionOpen,
        onConnectionClick = viewModel::onConnectionClick,
        isLoading = state.isLoading
    )
}

@Composable
private fun SettingsContent(
    isConnectionOpen: Boolean,
    onConnectionClick: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.size(48.dp))

        if (isConnectionOpen) {
            ShowConnection()
        } else {
            EnterConnectionId(
                isLoading = isLoading,
                onConnectionClick = onConnectionClick
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterConnectionId(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onConnectionClick: (String) -> Unit
) {
    var connectionIdText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    if (isLoading) {
        FullscreenCircularProgressContent()
    } else {
        TextField(
            value = connectionIdText,
            onValueChange = { connectionIdText = it },
            label = { Text("Enter the connection id") }
        )

        Spacer(modifier = modifier.size(12.dp))

        Button(
            onClick = {
                onConnectionClick(connectionIdText.text)
            }
        ) {
            Text(
                text = "Add Connection",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenWithoutActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsContent(
            isConnectionOpen = false,
            onConnectionClick = {},
            isLoading = false
        )
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun SettingsScreenWithActiveConnectionPreview() {
    BabylonWalletTheme {
        SettingsContent(
            isConnectionOpen = true,
            onConnectionClick = {},
            isLoading = false
        )
    }
}
