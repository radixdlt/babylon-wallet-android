package com.babylon.wallet.android.presentation.settings.preferences.ss.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample

@Composable
fun SignalingServersScreen(
    viewModel: SignalingServersViewModel,
    onBackClick: () -> Unit,
    onServerClick: (P2pTransportProfile) -> Unit,
    onAddServerClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SignalingServersContent(
        state = state,
        onBackClick = onBackClick,
        onAddClick = onAddServerClick,
        onItemClick = onServerClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage
    )
}

@Composable
private fun SignalingServersContent(
    state: SignalingServersViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (P2pTransportProfile) -> Unit,
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

            state.current?.let {
                item {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .padding(vertical = RadixTheme.dimensions.paddingSmall),
                        text = "Current",
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .background(color = RadixTheme.colors.card)
                            .padding(
                                start = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingSmall,
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault
                            )
                    ) {
                        SignalingServerView(
                            item = it,
                            modifier = Modifier
                                .throttleClickable { onItemClick(it) }
                                .fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
                }
            }

            if (state.items.isNotEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                            .padding(vertical = RadixTheme.dimensions.paddingSmall),
                        text = "Others",
                        style = RadixTheme.typography.body1HighImportance,
                        color = RadixTheme.colors.text,
                    )
                }
            }

            itemsIndexed(state.items) { index, item ->
                Column(
                    modifier = Modifier.background(color = RadixTheme.colors.card)
                ) {
                    SignalingServerView(
                        item = item,
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
    item: P2pTransportProfile,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.signalingServer,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(id = DSR.ic_chevron_right),
            contentDescription = null,
            tint = RadixTheme.colors.iconTertiary
        )
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
            onDismissErrorMessage = {}
        )
    }
}

@UsesSampleValues
class SignalingServersPreviewProvider : PreviewParameterProvider<SignalingServersViewModel.State> {

    override val values: Sequence<SignalingServersViewModel.State>
        get() = sequenceOf(
            SignalingServersViewModel.State(
                current = P2pTransportProfile.sample(),
                items = listOf(P2pTransportProfile.sample.other())
            ),
            SignalingServersViewModel.State()
        )
}
