package com.babylon.wallet.android.presentation.settings.preferences.radixconnect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.data.repository.radixconnect.IrohRelayServer
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.composable.RadixTextFieldDefaults
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.utils.copyToClipboard

@Composable
fun IrohRelayServerDetailsScreen(
    viewModel: IrohRelayServerDetailsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                IrohRelayServerDetailsViewModel.Event.Dismiss -> onBackClick()
            }
        }
    }

    IrohRelayServerDetailsContent(
        state = state,
        onBackClick = onBackClick,
        onRelayUrlChanged = viewModel::onRelayUrlChanged,
        onSaveClick = viewModel::onSaveClick,
        onMakeCurrentClick = viewModel::onMakeCurrentClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed,
        onDismissErrorMessage = viewModel::onDismissErrorMessage
    )
}

@Composable
private fun IrohRelayServerDetailsContent(
    state: IrohRelayServerDetailsViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onRelayUrlChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onMakeCurrentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit,
    onDismissErrorMessage: () -> Unit
) {
    if (state.showDeleteConfirmation) {
        BasicPromptAlertDialog(
            finish = onDeleteConfirmationDismissed,
            title = {
                Text(
                    text = stringResource(id = R.string.preferences_radixConnect_relay_deleteConfirmation_title),
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.text
                )
            },
            message = {
                Text(
                    text = stringResource(id = R.string.preferences_radixConnect_relay_deleteConfirmation_message),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(id = R.string.common_remove),
            confirmTextColor = RadixTheme.colors.error
        )
    }

    state.errorMessage?.let { errorMessage ->
        ErrorAlertDialog(errorMessage = errorMessage, cancel = onDismissErrorMessage)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.preferences_radixConnect_relay_title),
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        bottomBar = { BottomButtons(state, onSaveClick, onMakeCurrentClick, onDeleteClick) },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            if (state.isPublic) {
                PublicRelays(state)
            } else {
                RelayServerUrlField(state, onRelayUrlChanged)
            }
        }
    }
}

// The public relays laid out as WebRTC's signaling server: its name, then its servers' URLs.
@Composable
private fun PublicRelays(state: IrohRelayServerDetailsViewModel.State) {
    Column {
        EntryView(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            title = stringResource(id = R.string.preferences_radixConnect_relay_name)
        ) {
            RadixTextField(
                value = stringResource(id = R.string.preferences_radixConnect_relays_public),
                enabled = false,
                singleLine = true,
                onValueChanged = {},
                colors = readOnlyColors()
            )
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = RadixTheme.colors.background)
                .padding(RadixTheme.dimensions.paddingDefault)
        ) {
            Text(
                text = stringResource(id = R.string.preferences_radixConnect_relay_servers),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
            Text(
                text = stringResource(id = R.string.preferences_radixConnect_relays_publicInfo),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            val urls = state.publicRelayUrls
            if (urls == null) {
                if (state.isLoaded) UnavailableText()
            } else {
                EntryView(title = stringResource(id = R.string.preferences_radixConnect_relay_urls)) {
                    urls.forEach { url ->
                        CopyableUrl(
                            modifier = Modifier.padding(top = RadixTheme.dimensions.paddingXSmall),
                            url = url
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelayServerUrlField(
    state: IrohRelayServerDetailsViewModel.State,
    onRelayUrlChanged: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault)) {
        EntryView(title = stringResource(id = R.string.preferences_radixConnect_relay_url)) {
            RelayServerUrlTextField(state, onRelayUrlChanged)
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        if (state.isLoaded && !state.isLibraryAvailable) {
            UnavailableText()
        } else {
            Text(
                text = stringResource(id = R.string.preferences_radixConnect_relay_urlInfo),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun RelayServerUrlTextField(
    state: IrohRelayServerDetailsViewModel.State,
    onRelayUrlChanged: (String) -> Unit
) {
    RadixTextField(
        modifier = Modifier.fillMaxWidth(),
        value = state.relayUrl,
        onValueChanged = onRelayUrlChanged,
        enabled = state.isEditable,
        singleLine = true,
        hint = "https://",
        error = when (val fieldError = state.fieldError) {
            is IrohRelayServerDetailsViewModel.FieldError.Invalid ->
                stringResource(id = R.string.preferences_radixConnect_relay_invalid, fieldError.reason)

            IrohRelayServerDetailsViewModel.FieldError.AlreadyAdded ->
                stringResource(id = R.string.preferences_radixConnect_relay_alreadyAdded)

            null -> null
        },
        colors = readOnlyColors(),
        // A saved relay server can be copied, as a signaling server's URL can.
        trailingIcon = if (state.relayServer != null) {
            { CopyButton(state.relayUrl) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun CopyableUrl(url: String, modifier: Modifier = Modifier) {
    RadixTextField(
        modifier = modifier.fillMaxWidth(),
        value = url,
        enabled = false,
        singleLine = true,
        onValueChanged = {},
        colors = readOnlyColors(),
        trailingIcon = { CopyButton(url) }
    )
}

@Composable
private fun CopyButton(url: String) {
    val context = LocalContext.current
    val label = stringResource(id = R.string.preferences_radixConnect_relay_url)
    IconButton(onClick = { context.copyToClipboard(label, url) }) {
        Icon(
            painter = painterResource(id = DSR.ic_copy),
            contentDescription = null,
            tint = RadixTheme.colors.icon
        )
    }
}

@Composable
private fun UnavailableText() {
    Text(
        text = stringResource(id = R.string.preferences_radixConnect_relay_unavailable),
        style = RadixTheme.typography.body2Regular,
        color = RadixTheme.colors.error
    )
}

// What a field shows when it cannot be edited: as the signaling server's, not greyed out.
@Composable
private fun readOnlyColors() = RadixTextFieldDefaults.colors(
    disabledContainerColor = RadixTheme.colors.textFieldBackground,
    disabledTextColor = RadixTheme.colors.text,
    disabledBorderColor = RadixTheme.colors.textFieldBorder
)

@Composable
private fun EntryView(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        content()
    }
}

@Composable
private fun BottomButtons(
    state: IrohRelayServerDetailsViewModel.State,
    onSaveClick: () -> Unit,
    onMakeCurrentClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    RadixBottomBar(
        button = {
            if (state.isPublic) {
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.preferences_radixConnect_relay_makeCurrent),
                    onClick = onMakeCurrentClick,
                    enabled = state.canBeMadeCurrent
                )
            } else {
                RadixPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.common_save),
                    onClick = onSaveClick,
                    enabled = state.isSaveEnabled
                )
            }
        },
        additionalBottomContent = {
            if (!state.isPublic && state.relayServer != null && state.canBeMadeCurrent) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.preferences_radixConnect_relay_makeCurrent),
                    onClick = onMakeCurrentClick
                )
            }
            if (state.canBeRemoved) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
                RadixTextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = stringResource(id = R.string.preferences_radixConnect_relay_delete),
                    leadingIcon = {
                        Icon(painter = painterResource(id = DSR.ic_delete_outline), contentDescription = null)
                    },
                    onClick = onDeleteClick
                )
            }
        }
    )
}

@Preview
@Composable
private fun IrohRelayServerDetailsPreview() {
    RadixWalletPreviewTheme {
        IrohRelayServerDetailsContent(
            state = IrohRelayServerDetailsViewModel.State(
                relayServer = IrohRelayServer.Custom("https://relay.example.com"),
                relayUrl = "ftp://relay.example.com",
                isLoaded = true,
                isLibraryAvailable = true,
                fieldError = IrohRelayServerDetailsViewModel.FieldError.Invalid("the scheme must be https or http")
            ),
            onBackClick = {},
            onRelayUrlChanged = {},
            onSaveClick = {},
            onMakeCurrentClick = {},
            onDeleteClick = {},
            onDeleteConfirmationDismissed = {},
            onDismissErrorMessage = {}
        )
    }
}
