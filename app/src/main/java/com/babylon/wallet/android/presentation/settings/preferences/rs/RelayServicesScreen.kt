package com.babylon.wallet.android.presentation.settings.preferences.rs

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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.ErrorAlertDialog
import com.babylon.wallet.android.presentation.ui.composables.RadixBottomBar
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.RelayService
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toUrl

@Composable
fun RelayServicesScreen(
    viewModel: RelayServicesViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RelayServicesContent(
        state = state,
        onBackClick = onBackClick,
        onItemClick = viewModel::onItemClick,
        onDismissErrorMessage = viewModel::onDismissErrorMessage,
        onAddClick = { viewModel.setAddSheetVisible(true) },
        onDeleteItemClick = viewModel::onDeleteItemClick
    )

    state.addInput?.let { input ->
        AddRelayServiceSheet(
            input = input,
            onAddClick = viewModel::onAddConfirmed,
            onNameChanged = viewModel::onNewNameChanged,
            onUrlChanged = viewModel::onNewUrlChanged,
            onDismiss = { viewModel.setAddSheetVisible(false) }
        )
    }
}

@Composable
private fun RelayServicesContent(
    state: RelayServicesViewModel.State,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onItemClick: (RelayServicesViewModel.State.UiItem) -> Unit,
    onDeleteItemClick: (RelayServicesViewModel.State.UiItem) -> Unit,
    onAddClick: () -> Unit,
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
                    title = "Relay Services",
                    onBackClick = onBackClick,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        bottomBar = {
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
                    text = "Choose and manage relay services used for WalletConnect P2P.",
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.textSecondary
                )
            }

            itemsIndexed(state.items) { index, item ->
                Column(
                    modifier = Modifier.background(color = RadixTheme.colors.card)
                ) {
                    RelayServiceView(
                        modifier = Modifier
                            .throttleClickable { onItemClick(item) }
                            .fillMaxWidth()
                            .padding(
                                start = RadixTheme.dimensions.paddingDefault,
                                end = RadixTheme.dimensions.paddingSmall,
                                top = RadixTheme.dimensions.paddingDefault,
                                bottom = RadixTheme.dimensions.paddingDefault
                            ),
                        item = item,
                        onDeleteClick = { onDeleteItemClick(item) }
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

            if (state.items.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        color = RadixTheme.colors.divider,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))

                RadixSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault),
                    text = "Add New Relay Service",
                    onClick = onAddClick
                )
            }
        }
    }
}

@Composable
private fun RelayServiceView(
    item: RelayServicesViewModel.State.UiItem,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        if (item.isCurrent) {
            Icon(
                modifier = Modifier.width(24.dp),
                painter = painterResource(id = DSR.ic_check),
                contentDescription = null,
                tint = RadixTheme.colors.icon
            )
        } else {
            Box(
                modifier = Modifier.width(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.relayService.name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.relayService.url.toString(),
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                painter = painterResource(id = DSR.ic_delete_outline),
                tint = RadixTheme.colors.icon,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun AddRelayServiceSheet(
    input: RelayServicesViewModel.State.AddInput,
    onAddClick: () -> Unit,
    onNameChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    BottomSheetDialogWrapper(
        addScrim = true,
        showDragHandle = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Add New Relay Service",
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSemiLarge))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Enter a relay service name and URL",
                style = RadixTheme.typography.body1Regular,
                color = RadixTheme.colors.text,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                    .focusRequester(inputFocusRequester),
                onValueChanged = onNameChanged,
                value = input.name,
                hint = "Enter name",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingXXLarge),
                onValueChanged = onUrlChanged,
                value = input.url,
                hint = stringResource(id = R.string.gateways_addNewGateway_textFieldPlaceholder),
                singleLine = true,
                error = when (input.failure) {
                    RelayServicesViewModel.State.AddInput.Failure.AlreadyExist -> "This relay service is already added"

                    else -> null
                }
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXXLarge))
        }

        RadixBottomBar(
            onClick = onAddClick,
            text = "Add Relay Service",
            enabled = input.isUrlValid,
            isLoading = input.isLoading,
            insets = WindowInsets.navigationBars.union(WindowInsets.ime)
        )
    }
}

@Composable
@Preview
@UsesSampleValues
private fun RelayServicesPreview(
    @PreviewParameter(RelayServicesPreviewProvider::class) state: RelayServicesViewModel.State
) {
    RadixWalletPreviewTheme {
        RelayServicesContent(
            state = state,
            onBackClick = {},
            onAddClick = {},
            onItemClick = {},
            onDeleteItemClick = {},
            onDismissErrorMessage = {},
        )
    }
}

@UsesSampleValues
class RelayServicesPreviewProvider : PreviewParameterProvider<RelayServicesViewModel.State> {

    override val values: Sequence<RelayServicesViewModel.State>
        get() = sequenceOf(
            RelayServicesViewModel.State(
                items = listOf(
                    RelayServicesViewModel.State.UiItem(
                        RelayService(
                            name = "Relay Service 1",
                            url = "https://test.com".toUrl()
                        ),
                        isCurrent = true
                    )
                )
            ),
            RelayServicesViewModel.State()
        )
}
