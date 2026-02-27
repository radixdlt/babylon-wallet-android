package com.babylon.wallet.android.presentation.settings.preferences.ss

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BasicPromptAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
fun SignalingServersScreen(
    viewModel: SignalingServersViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignalingServersContent(
        state = state,
        onBackClick = onBackClick,
        onAddClick = viewModel::onAddClick,
        onItemClick = viewModel::onItemClick,
        onDeleteItemClick = viewModel::onDeleteItemClick,
        onDeleteConfirmationDismissed = viewModel::onDeleteConfirmationDismissed
    )
}

@Composable
private fun SignalingServersContent(
    state: SignalingServersViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (SignalingServersViewModel.State.UiItem) -> Unit,
    onDeleteItemClick: (SignalingServersViewModel.State.UiItem) -> Unit,
    onDeleteConfirmationDismissed: (Boolean) -> Unit
) {
    state.itemToDelete?.let {
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

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                RadixCenteredTopAppBar(
                    title = "Signaling Servers",
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingMedium
                    ),
                    text = "Choose and manage signaling server profiles used for WalletConnect P2P.",
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.textSecondary
                )
            }

            itemsIndexed(state.items) { index, item ->
                Column(
                    modifier = Modifier.background(color = RadixTheme.colors.card)
                ) {
                    SignalingServerView(
                        item = item,
                        onDeleteClick = onDeleteItemClick,
                        modifier = Modifier
                            .throttleClickable { onItemClick(item) }
                            .fillMaxWidth()
                            .padding(
                                start = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingSmall,
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault
                            )
                    )

                    if (remember(state.items.size) { index < state.items.size - 1 }) {
                        HorizontalDivider(
                            color = RadixTheme.colors.divider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = "Add New Signaling Server",
                    onClick = onAddClick
                )
            }
        }
    }
}

@Composable
private fun SignalingServerView(
    item: SignalingServersViewModel.State.UiItem,
    onDeleteClick: (SignalingServersViewModel.State.UiItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        if (item.selected) {
            Icon(
                modifier = Modifier.width(24.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_check),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )
        } else {
            Box(modifier = Modifier.width(24.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.server.name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.url,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = {
                onDeleteClick(item)
            }
        ) {
            Icon(
                painter = painterResource(
                    id = DSR.ic_delete_outline
                ),
                tint = RadixTheme.colors.icon,
                contentDescription = null
            )
        }
    }
}

@Composable
@Preview
@UsesSampleValues
private fun SignalingServersPreview(
    @PreviewParameter(SignalingServersPreviewProvider::class) state: SignalingServersViewModel.State
) {
    RadixWalletPreviewTheme {
        SignalingServersContent(
            state = state,
            onBackClick = {},
            onAddClick = {},
            onItemClick = {},
            onDeleteItemClick = {},
            onDeleteConfirmationDismissed = {}
        )
    }
}

@UsesSampleValues
class SignalingServersPreviewProvider : PreviewParameterProvider<SignalingServersViewModel.State> {

    override val values: Sequence<SignalingServersViewModel.State>
        get() = sequenceOf(
            SignalingServersViewModel.State(
                items = P2pTransportProfile.sample.all.mapIndexed { index, item ->
                    SignalingServersViewModel.State.UiItem(
                        server = item,
                        selected = index == 0
                    )
                }
            ),
            SignalingServersViewModel.State()
        )
}
