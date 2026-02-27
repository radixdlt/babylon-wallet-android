package com.babylon.wallet.android.presentation.settings.preferences.ss.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

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
        onDeleteClick = viewModel::onDeleteClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed,
        onSaveClick = viewModel::onSaveClick,
        onChangeCurrentClick = viewModel::onChangeAsCurrent
    )
}

@Composable
private fun SignalingServerDetailsContent(
    state: SignalingServerDetailsViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit,
    onDismissErrorMessage: () -> Unit,
    onSaveClick: () -> Unit,
    onChangeCurrentClick: () -> Unit
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
                        onClick = onSaveClick
                    )
                },
                additionalBottomContent = {
                    if (state.editMode?.isCurrent == false) {
                        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                        RadixSecondaryButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                            text = "Mark as current",
                            onClick = onChangeCurrentClick
                        )
                    }

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
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding)
        ) {
            when (val mode = state.mode) {
                is SignalingServerDetailsViewModel.State.Mode.Add -> AddModeContent(
                    data = mode
                )

                is SignalingServerDetailsViewModel.State.Mode.Edit -> EditModeContent(
                    data = mode
                )

                else -> null
            }
        }
    }
}

@Composable
private fun AddModeContent(
    data: SignalingServerDetailsViewModel.State.Mode.Add
) {
}

@Composable
private fun EditModeContent(
    data: SignalingServerDetailsViewModel.State.Mode.Edit
) {
    Column(
        modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            text = data.p2pTransportProfile.name,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = data.p2pTransportProfile.signalingServer,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // TODO display details
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
            onDeleteClick = {},
            onDeleteConfirmationDismissed = {},
            onDismissErrorMessage = {},
            onBackClick = {},
            onSaveClick = {},
            onChangeCurrentClick = {}
        )
    }
}

@UsesSampleValues
class SignalingServerDetailsPreviewProvider : PreviewParameterProvider<SignalingServerDetailsViewModel.State> {

    override val values: Sequence<SignalingServerDetailsViewModel.State>
        get() = sequenceOf(
            SignalingServerDetailsViewModel.State(
                mode = SignalingServerDetailsViewModel.State.Mode.Edit(
                    p2pTransportProfile = P2pTransportProfile.sample(),
                    isCurrent = true
                )
            ),
            SignalingServerDetailsViewModel.State(
                mode = SignalingServerDetailsViewModel.State.Mode.Add
            )
        )
}
