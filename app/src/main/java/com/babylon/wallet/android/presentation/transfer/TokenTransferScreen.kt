package com.babylon.wallet.android.presentation.transfer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size.Companion.Zero
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.toSize
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUiMessageHandler
import com.babylon.wallet.android.utils.truncatedHash
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import rdx.works.profile.data.model.pernetwork.OnNetwork

@Composable
fun TokenTransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TokenTransferViewModel,
    onBackClick: () -> Unit
) {
    val state = viewModel.state

    TokenTransferContent(
        modifier = modifier,
        accounts = state.accounts,
        onBackClick = onBackClick,
        senderAddress = state.senderAddress,
        onSenderAddressChanged = viewModel::onSenderAddressChanged,
        recipientAddress = state.recipientAddress,
        onRecipientAddressChanged = viewModel::onRecipientAddressChanged,
        tokenAmount = state.tokenAmount,
        onTokenAmountChanged = viewModel::onTokenAmountChanged,
        buttonEnabled = state.buttonEnabled,
        onTransferClick = viewModel::onTransferClick,
        isLoading = state.isLoading,
        transferComplete = state.transferComplete,
        error = state.error,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun TokenTransferContent(
    modifier: Modifier,
    accounts: ImmutableList<OnNetwork.Account>,
    onBackClick: () -> Unit,
    senderAddress: String,
    onSenderAddressChanged: (String) -> Unit,
    recipientAddress: String,
    onRecipientAddressChanged: (String) -> Unit,
    tokenAmount: String,
    onTokenAmountChanged: (String) -> Unit,
    buttonEnabled: Boolean,
    onTransferClick: () -> Unit,
    isLoading: Boolean,
    transferComplete: Boolean,
    onMessageShown: () -> Unit,
    error: UiMessage?,
) {
    var senderExpanded by rememberSaveable { mutableStateOf(false) }
    var recipientExpanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .background(RadixTheme.colors.defaultBackground)
                .fillMaxSize()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painterResource(id = R.drawable.ic_close),
                    tint = RadixTheme.colors.gray1,
                    contentDescription = "navigate back"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingMedium
                ),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Text("From")
                DropdownTextField(
                    items = accounts.map { it.address }.toImmutableList(),
                    expanded = senderExpanded,
                    onExpandedChanged = { senderExpanded = it },
                    selectedText = senderAddress,
                    onSelectedTextChanged = onSenderAddressChanged
                )
            }

            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = RadixTheme.dimensions.paddingLarge,
                    vertical = RadixTheme.dimensions.paddingMedium
                ),
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Text("To")
                DropdownTextField(
                    items = accounts.map { it.address }.toImmutableList(),
                    expanded = recipientExpanded,
                    onExpandedChanged = { recipientExpanded = it },
                    selectedText = recipientAddress,
                    onSelectedTextChanged = onRecipientAddressChanged
                )
            }

            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

            RadixTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                value = tokenAmount,
                onValueChanged = onTokenAmountChanged,
                hint = "Amount",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.size(RadixTheme.dimensions.paddingMedium))

            if (isLoading) {
                FullscreenCircularProgressContent()
            }

            RadixPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium),
                text = "Transfer",
                onClick = onTransferClick,
                enabled = buttonEnabled
            )

            AnimatedVisibility(visible = transferComplete) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.R.drawable.img_dapp_complete),
                        contentDescription = null
                    )
                    Text(
                        text = "Transfer Complete",
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2,
                    )
                }
            }
        }
        SnackbarUiMessageHandler(
            message = error,
            onMessageShown = onMessageShown,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
fun DropdownTextField(
    modifier: Modifier = Modifier,
    items: ImmutableList<String>,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    selectedText: String,
    onSelectedTextChanged: (String) -> Unit,
) {
    var textFieldSize by remember { mutableStateOf(Zero) }

    val icon = if (expanded) {
        R.drawable.ic_arrow_sent_upwards
    } else {
        R.drawable.ic_arrow_received_downward
    }

    Column {
        RadixTextField(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // This value is used to assign to the DropDown the same width
                    textFieldSize = coordinates.size.toSize()
                },
            onValueChanged = onSelectedTextChanged,
            value = selectedText,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = icon),
                    "contentDescription",
                    Modifier.clickable { onExpandedChanged(!expanded) }
                )
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChanged(false) },
            modifier = Modifier
                .width(
                    with(LocalDensity.current) {
                        textFieldSize.width.toDp()
                    }
                )
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedTextChanged(item)
                        onExpandedChanged(!expanded)
                    }
                ) {
                    Text(text = item.truncatedHash())
                }
            }
        }
    }
}

@Composable
@Preview
fun TokenTransferContentPreview() {
    TokenTransferContent(
        modifier = Modifier,
        accounts = persistentListOf(),
        onBackClick = {},
        senderAddress = "1qzxcrac59cy2v9lpcpmf8gqf6rug7",
        onSenderAddressChanged = {},
        recipientAddress = "1qzxcrac59cy2v9lpcpmf8gqf6rug7",
        onRecipientAddressChanged = {},
        tokenAmount = "100",
        onTokenAmountChanged = {},
        buttonEnabled = true,
        onTransferClick = {},
        transferComplete = false,
        isLoading = false,
        onMessageShown = {},
        error = null
    )
}

@Composable
@Preview
fun DropdownTextFieldPreview() {
    DropdownTextField(
        items = listOf("Item1", "Item2", "Item3").toImmutableList(),
        expanded = false,
        onExpandedChanged = {},
        selectedText = "",
        onSelectedTextChanged = {}
    )
}
