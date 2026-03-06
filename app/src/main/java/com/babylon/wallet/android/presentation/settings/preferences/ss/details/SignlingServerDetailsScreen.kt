package com.babylon.wallet.android.presentation.settings.preferences.ss.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
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
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.copyToClipboard
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SignalingServerDetailsScreen(
    viewModel: SignalingServerDetailsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                SignalingServerDetailsViewModel.Event.Dismiss -> onBackClick()
            }
        }
    }

    SignalingServerDetailsContent(
        state = state,
        onBackClick = onBackClick,
        onNameChanged = viewModel::onNameChanged,
        onSignalingServerUrlChanged = viewModel::onSignalingServerUrlChanged,
        onStunUrlChanged = viewModel::onStunUrlChanged,
        onTurnUrlChanged = viewModel::onTurnUrlChanged,
        onDeleteStunUrlClick = viewModel::onDeleteStunUrlClick,
        onDeleteTurnUrlClick = viewModel::onDeleteTurnUrlClick,
        onAddStunUrlClick = viewModel::onAddStunUrlClick,
        onAddTurnUrlClick = viewModel::onAddTurnUrlClick,
        onTurnUsernameChanged = viewModel::onTurnUsernameChanged,
        onTurnPasswordChanged = viewModel::onTurnPasswordChanged,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed,
        onSaveClick = viewModel::onSaveClick,
        onChangeCurrentClick = viewModel::onChangeAsCurrent,
    )
}

@Composable
private fun SignalingServerDetailsContent(
    state: SignalingServerDetailsViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onDismissErrorMessage: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSignalingServerUrlChanged: (String) -> Unit,
    onStunUrlChanged: (Int, String) -> Unit,
    onTurnUrlChanged: (Int, String) -> Unit,
    onDeleteStunUrlClick: (Int) -> Unit,
    onDeleteTurnUrlClick: (Int) -> Unit,
    onAddStunUrlClick: () -> Unit,
    onAddTurnUrlClick: () -> Unit,
    onTurnUsernameChanged: (String) -> Unit,
    onTurnPasswordChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit,
    onChangeCurrentClick: () -> Unit,
) {
    if (state.showDeleteConfirmation) {
        BasicPromptAlertDialog(
            finish = onDeleteConfirmationDismissed,
            title = {
                Text(
                    text = "Remove Signaling Server",
                    style = RadixTheme.typography.body2Header,
                    color = RadixTheme.colors.text
                )
            },
            message = {
                Text(
                    text = "You will no longer be able to connect to this signaling server.",
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.text
                )
            },
            confirmText = stringResource(id = R.string.common_remove),
            confirmTextColor = RadixTheme.colors.error
        )
    }

    state.errorMessage?.let {
        ErrorAlertDialog(
            errorMessage = it,
            cancel = onDismissErrorMessage
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = "Signaling Server",
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        bottomBar = {
            RadixBottomBar(
                button = {
                    RadixPrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                        text = "Save",
                        onClick = onSaveClick,
                        enabled = state.isSaveEnabled
                    )
                },
                additionalBottomContent = {
                    if (state.isInEditMode && !state.isCurrent) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                        RadixSecondaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "Change as current",
                            onClick = onChangeCurrentClick
                        )
                    }

                    if (state.isInEditMode) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                        RadixTextButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "Delete",
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = DSR.ic_delete_outline),
                                    contentDescription = null
                                )
                            },
                            onClick = onDeleteClick
                        )
                    }
                }
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            EntryView(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                title = "Signaling Server Name"
            ) {
                RadixTextField(
                    value = state.name,
                    enabled = !state.isInEditMode,
                    singleLine = true,
                    onValueChanged = onNameChanged,
                    colors = RadixTextFieldDefaults.colors(
                        disabledContainerColor = RadixTheme.colors.textFieldBackground,
                        disabledTextColor = RadixTheme.colors.text,
                        disabledBorderColor = RadixTheme.colors.textFieldBorder
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            EntryView(
                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
                title = "Signaling Server Url"
            ) {
                val context = LocalContext.current

                RadixTextField(
                    modifier = Modifier.throttleClickable {
                        context.copyToClipboard("Signaling Server Url", state.url)
                    },
                    value = state.url,
                    enabled = !state.isInEditMode,
                    onValueChanged = onSignalingServerUrlChanged,
                    colors = RadixTextFieldDefaults.colors(
                        disabledContainerColor = RadixTheme.colors.textFieldBackground,
                        disabledTextColor = RadixTheme.colors.text,
                        disabledBorderColor = RadixTheme.colors.textFieldBorder
                    ),
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = DSR.ic_copy),
                            contentDescription = null,
                            tint = RadixTheme.colors.icon
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            ServerSectionView(
                title = "Stun Server",
                description = "STUN servers are used to find the public facing IP address of each peer.",
                urls = state.stunUrls,
                canAddMore = state.canAddMoreStunUrls,
                onUrlChanged = onStunUrlChanged,
                onDeleteUrlClick = onDeleteStunUrlClick,
                onAddMoreClick = onAddStunUrlClick
            )

            ServerSectionView(
                title = "Turn Server",
                description = "If STUN servers fail, then a TURN server is used instead as a proxy fallback.",
                username = state.turnUsername,
                password = state.turnPassword,
                urls = state.turnUrls,
                canAddMore = state.canAddMoreTurnUrls,
                onUrlChanged = onTurnUrlChanged,
                onDeleteUrlClick = onDeleteTurnUrlClick,
                onAddMoreClick = onAddTurnUrlClick,
                onUsernameChanged = onTurnUsernameChanged,
                onPasswordChanged = onTurnPasswordChanged
            )
        }
    }
}

@Composable
private fun ServerSectionView(
    title: String,
    description: String,
    urls: List<String>,
    onUrlChanged: (Int, String) -> Unit,
    onDeleteUrlClick: (Int) -> Unit,
    canAddMore: Boolean,
    onAddMoreClick: () -> Unit,
    username: String? = null,
    password: String? = null,
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.background
            )
            .padding(vertical = RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = title,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        Text(
            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingDefault),
            text = description,
            style = RadixTheme.typography.body2Regular,
            color = RadixTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

        username?.let {
            EntryView(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = 48.dp
                ),
                title = "Username"
            ) {
                RadixTextField(
                    value = it,
                    singleLine = true,
                    onValueChanged = onUsernameChanged,
                    colors = RadixTextFieldDefaults.colors(
                        disabledContainerColor = RadixTheme.colors.textFieldBackground,
                        disabledTextColor = RadixTheme.colors.text,
                        disabledBorderColor = RadixTheme.colors.textFieldBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        }

        password?.let {
            EntryView(
                modifier = Modifier.padding(
                    start = RadixTheme.dimensions.paddingDefault,
                    end = 48.dp
                ),
                title = "Password"
            ) {
                RadixTextField(
                    value = it,
                    singleLine = true,
                    onValueChanged = onPasswordChanged,
                    colors = RadixTextFieldDefaults.colors(
                        disabledContainerColor = RadixTheme.colors.textFieldBackground,
                        disabledTextColor = RadixTheme.colors.text,
                        disabledBorderColor = RadixTheme.colors.textFieldBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        }

        EntryView(
            modifier = Modifier.padding(
                start = RadixTheme.dimensions.paddingDefault,
                top = RadixTheme.dimensions.paddingSmall
            ),
            title = "URLs"
        ) {
            urls.forEachIndexed { index, url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = RadixTheme.dimensions.paddingXSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadixTextField(
                        modifier = Modifier.weight(1f),
                        value = url,
                        onValueChanged = { onUrlChanged(index, it) },
                        singleLine = true
                    )

                    IconButton(
                        onClick = { onDeleteUrlClick(index) }
                    ) {
                        Icon(
                            painter = painterResource(id = DSR.ic_delete_outline),
                            contentDescription = null,
                            tint = RadixTheme.colors.iconSecondary
                        )
                    }
                }
            }
        }

        if (canAddMore) {
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            RadixSecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = RadixTheme.dimensions.paddingDefault,
                        end = 44.dp
                    )
                    .height(41.dp),
                text = stringResource(id = R.string.plus),
                textStyle = RadixTheme.typography.header,
                onClick = onAddMoreClick
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
    }
}

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
@Preview
@UsesSampleValues
private fun SignalingServerPreview(
    @PreviewParameter(SignalingServerDetailsPreviewProvider::class) state: SignalingServerDetailsViewModel.State
) {
    RadixWalletPreviewTheme {
        SignalingServerDetailsContent(
            state = state,
            onNameChanged = {},
            onSignalingServerUrlChanged = {},
            onStunUrlChanged = { _, _ -> },
            onTurnUrlChanged = { _, _ -> },
            onDeleteStunUrlClick = {},
            onDeleteTurnUrlClick = {},
            onAddStunUrlClick = {},
            onAddTurnUrlClick = {},
            onTurnUsernameChanged = {},
            onTurnPasswordChanged = {},
            onDeleteClick = {},
            onDeleteConfirmationDismissed = {},
            onDismissErrorMessage = {},
            onBackClick = {},
            onSaveClick = {},
            onChangeCurrentClick = {},
        )
    }
}

@UsesSampleValues
class SignalingServerDetailsPreviewProvider : PreviewParameterProvider<SignalingServerDetailsViewModel.State> {

    override val values: Sequence<SignalingServerDetailsViewModel.State>
        get() = sequenceOf(
            SignalingServerDetailsViewModel.State(
                isInEditMode = true,
                name = "Radix Production",
                url = "wss://signaling-server-dev.rdx-works-main.extratools.works/",
                stunUrls = persistentListOf(
                    "stun:stun.l.google.com:19302",
                ),
                turnUsername = "username",
                turnPassword = "password",
                turnUrls = persistentListOf(
                    "turn:turn-udp.radixdlt.com:80?transport=udp",
                ),
                isCurrent = false
            ),
            SignalingServerDetailsViewModel.State(
                isInEditMode = true,
                name = "Radix Development",
                url = "wss://signaling-server-dev.rdx-works-main.extratools.works/",
                stunUrls = persistentListOf(
                    "stun:stun.l.google.com:19302",
                    "stun:stun.l.google.com:19302",
                    "stun:stun.l.google.com:19302"
                ),
                turnUrls = persistentListOf(
                    "turn:turn-udp.radixdlt.com:80?transport=udp",
                    "turn:turn-udp.radixdlt.com:80?transport=udp",
                ),
                isCurrent = true
            ),
            SignalingServerDetailsViewModel.State(
                isInEditMode = false
            )
        )
}
