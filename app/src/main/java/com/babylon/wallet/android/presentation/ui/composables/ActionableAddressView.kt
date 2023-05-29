package com.babylon.wallet.android.presentation.ui.composables

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalTextStyle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.model.ActionableAddress
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: String,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val actionableAddress = resolveAddress(address = address)
    val actions = resolveActions(actionableAddress = actionableAddress)
    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
    var onAction: OnAction? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(onAction) {
        if (onAction == null) {
            scope.launch { sheetState.hide() }
        } else if (onAction?.isPresentedInModal == true) {
            scope.launch { sheetState.show() }
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        onAction = actions.primary.onAction()
                    },
                    onLongClick = { isDropdownMenuExpanded = true }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXSmall)
        ) {
            Text(
                text = actionableAddress.displayAddress,
                color = textColor,
                maxLines = 1,
                style = textStyle
            )

            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(id = actions.primary.icon),
                contentDescription = actions.primary.name,
                tint = iconColor,
            )
        }

        DropdownMenu(
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            expanded = isDropdownMenuExpanded,
            onDismissRequest = { isDropdownMenuExpanded = false }
        ) {
            actions.all.forEach {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = it.name,
                            style = RadixTheme.typography.body1Regular,
                            color = RadixTheme.colors.defaultText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = it.icon),
                            contentDescription = it.name,
                            tint = RadixTheme.colors.defaultText
                        )
                    },
                    onClick = {
                        isDropdownMenuExpanded = false
                        onAction = it.onAction()
                    },
                    contentPadding = PaddingValues(
                        horizontal = RadixTheme.dimensions.paddingDefault,
                        vertical = RadixTheme.dimensions.paddingXSmall
                    )
                )
            }
        }

        onAction?.let { actionData ->
            if (actionData.isPresentedInModal) {
                BottomSheetWrapper(
                    onDismissRequest = { onAction = null },
                    bottomSheetState = sheetState
                ) {
                    actionData.ActionView()
                }
            } else {
                actionData.ActionView()
                onAction = null
            }
        }
    }
}

@Composable
private fun resolveAddress(
    address: String
): ActionableAddress = remember(address) { ActionableAddress.from(address) }

@Composable
private fun resolveActions(
    actionableAddress: ActionableAddress
): PopupActions {
    val copyAction = PopupActionItem(
        name = stringResource(
            id = when {
                actionableAddress.type == ActionableAddress.Type.TRANSACTION -> R.string.addressAction_copyTransactionId
                actionableAddress.isNft -> R.string.addressAction_copyNftId
                else -> R.string.addressAction_copyAddress
            }
        ),
        icon = R.drawable.ic_copy
    ) { OnAction.CopyToClipboard(actionableAddress) }

    val openExternalAction = PopupActionItem(
        name = stringResource(id = R.string.addressAction_viewOnDashboard),
        icon = R.drawable.ic_external_link
    ) { OnAction.OpenExternalWebView(actionableAddress) }

    val qrAction = PopupActionItem(
        name = stringResource(id = R.string.action_show_qr_code),
        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
    ) { OnAction.QRCode(actionableAddress) }

    return remember(actionableAddress) {
        if (actionableAddress.isCopyPrimaryAction) {
            val secondaryActions = if (actionableAddress.type == ActionableAddress.Type.ACCOUNT) {
                listOf(qrAction, openExternalAction)
            } else {
                listOf(openExternalAction)
            }

            PopupActions(
                primary = copyAction,
                secondary = secondaryActions
            )
        } else {
            PopupActions(
                primary = openExternalAction,
                secondary = listOf(copyAction)
            )
        }
    }
}

private data class PopupActions(
    val primary: PopupActionItem,
    val secondary: List<PopupActionItem>
) {

    val all = listOf(primary) + secondary
}

private data class PopupActionItem(
    val name: String,
    @DrawableRes val icon: Int,
    val onAction: () -> OnAction
)

/**
 * An action that will be presented to the user when clicked
 * on the PopupMenu
 */
private sealed class OnAction {

    abstract val isPresentedInModal: Boolean

    /**
     * This view will appear when the user has clicked on the corresponding [PopupActionItem]
     */
    @Composable
    abstract fun ActionView()

    data class CopyToClipboard(val actionableAddress: ActionableAddress) : OnAction() {

        override val isPresentedInModal: Boolean = false

        @Composable
        override fun ActionView() {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current

            clipboardManager.setText(AnnotatedString(actionableAddress.address))

            // From Android 13, the system handles the copy confirmation
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
        }
    }

    data class OpenExternalWebView(val actionableAddress: ActionableAddress) : OnAction() {

        override val isPresentedInModal: Boolean = false

        @Suppress("SwallowedException")
        @Composable
        override fun ActionView() {
            val context = LocalContext.current

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = actionableAddress.toDashboardUrl().toUri()
            }

            try {
                context.startActivity(intent)
            } catch (activityNotFound: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    R.string.addressAction_noWebBrowserInstalled,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    data class QRCode(val actionableAddress: ActionableAddress) : OnAction() {

        override val isPresentedInModal: Boolean = true

        @Composable
        override fun ActionView() {
            Box(
                modifier = Modifier
                    .background(RadixTheme.colors.defaultBackground)
            ) {
                AccountQRCodeView(accountAddress = actionableAddress.address)
            }
        }
    }
}
