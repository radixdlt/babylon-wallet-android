package com.babylon.wallet.android.presentation.ui.composables

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.getSystemService
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.babylon.wallet.android.presentation.model.ActionableAddress
import com.babylon.wallet.android.utils.openUrl
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.gateways
import timber.log.Timber

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActionableAddressView(
    address: String,
    modifier: Modifier = Modifier,
    truncateAddress: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val actionableAddress = resolveAddress(address = address, truncateAddress = truncateAddress)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var actions: PopupActions? by remember(actionableAddress) {
        mutableStateOf(null)
    }

    val useCaseProvider = remember(context) {
        EntryPoints.get(context.applicationContext, ActionableAddressViewEntryPoint::class.java)
    }

    LaunchedEffect(actionableAddress) {
        scope.launch {
            val networkId = if (useCaseProvider.profileUseCase().isInitialized()) {
                useCaseProvider.profileUseCase().gateways.first().current().network.networkId()
            } else {
                Radix.Network.mainnet.networkId()
            }
            val copyAction = PopupActionItem(
                name = context.getString(
                    when {
                        actionableAddress.type == ActionableAddress.Type.Global.TRANSACTION -> R.string.addressAction_copyTransactionId
                        actionableAddress.isNft || actionableAddress.type is ActionableAddress.Type.LocalId ->
                            R.string.addressAction_copyNftId

                        else -> R.string.addressAction_copyAddress
                    }
                ),
                icon = R.drawable.ic_copy
            ) { OnAction.CallbackBasedAction.CopyToClipboard(actionableAddress) }

            val openExternalAction = actionableAddress.toDashboardUrl(networkId)?.let { url ->
                PopupActionItem(
                    name = context.getString(R.string.addressAction_viewOnDashboard),
                    icon = R.drawable.ic_external_link,
                ) {
                    OnAction.CallbackBasedAction.OpenExternalWebView(url)
                }
            }

            val qrAction = if (actionableAddress.type == ActionableAddress.Type.Global.ACCOUNT) {
                PopupActionItem(
                    name = context.getString(R.string.addressAction_showAccountQR),
                    icon = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                ) { OnAction.ViewBasedAction.QRCode(actionableAddress) }
            } else {
                null
            }

            val primary = if (actionableAddress.type == ActionableAddress.Type.Global.TRANSACTION && openExternalAction != null) {
                openExternalAction
            } else {
                copyAction
            }
            val secondary = listOf(
                qrAction,
                if (primary == openExternalAction) null else openExternalAction
            ).mapNotNull { it }

            actions = PopupActions(
                primary = primary,
                secondary = secondary
            )

            // Resolve if address is ledger and attach another action
            if (actionableAddress.type == ActionableAddress.Type.Global.ACCOUNT) {
                if (useCaseProvider.profileUseCase().accountOnCurrentNetwork(actionableAddress.address)?.isLedgerAccount == true) {
                    val verifyOnLedgerAction = PopupActionItem(
                        name = context.getString(R.string.addressAction_verifyAddressLedger),
                        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_ledger_hardware_wallets
                    ) {
                        OnAction.CallbackBasedAction.VerifyAddressOnLedger(
                            actionableAddress = actionableAddress,
                            verifyAddressOnLedgerUseCase = useCaseProvider.verifyAddressOnLedgerUseCase(),
                            applicationScope = useCaseProvider.applicationScope()
                        )
                    }

                    actions = actions?.copy(
                        secondary = actions?.secondary?.toMutableList()?.apply {
                            this.add(verifyOnLedgerAction)
                        }?.toList().orEmpty()
                    )
                }
            }
        }
    }

    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
    var viewBasedAction: OnAction.ViewBasedAction? by remember { mutableStateOf(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewBasedAction) {
        if (viewBasedAction == null) {
            scope.launch { sheetState.hide() }
        } else {
            scope.launch { sheetState.show() }
        }
    }
    Box(modifier = modifier) {
        ConstraintLayout(
            modifier = Modifier.combinedClickable(
                onClick = {
                    actions?.let { popupActions ->
                        when (val actionData = popupActions.primary.onAction()) {
                            is OnAction.CallbackBasedAction -> actionData.onAction(context)
                            is OnAction.ViewBasedAction -> viewBasedAction = actionData
                        }
                    }
                },
                onLongClick = { isDropdownMenuExpanded = true }
            )
        ) {
            val textRef = createRef()
            val iconRef = if (actions != null && truncateAddress) {
                createRef()
            } else {
                null
            }

            val inlineContentId = "icon"
            val inlinedText = buildAnnotatedString {
                append(actionableAddress.displayAddress)
                if (!truncateAddress) {
                    append(" ")
                    appendInlineContent(inlineContentId)
                }
            }
            val inlineContent = mapOf(
                inlineContentId to InlineTextContent(Placeholder(14.sp, 14.sp, PlaceholderVerticalAlign.Center)) {
                    actions?.let { popupActions ->
                        Icon(
                            modifier = Modifier.size(14.dp),
                            painter = painterResource(id = popupActions.primary.icon),
                            contentDescription = popupActions.primary.name,
                            tint = iconColor,
                        )
                    }
                }
            )

            Text(
                modifier = Modifier.constrainAs(textRef) {
                    start.linkTo(parent.start)
                    if (iconRef != null) {
                        end.linkTo(iconRef.start)
                    } else {
                        end.linkTo(parent.end)
                    }
                },
                text = inlinedText,
                color = textColor,
                maxLines = if (truncateAddress) 1 else 2,
                style = textStyle,
                overflow = if (truncateAddress) TextOverflow.Ellipsis else TextOverflow.Clip,
                inlineContent = inlineContent
            )

            actions?.let { popupActions ->
                if (iconRef != null) {
                    val iconMargin = RadixTheme.dimensions.paddingSmall
                    Icon(
                        modifier = Modifier.constrainAs(iconRef) {
                            start.linkTo(textRef.end, margin = iconMargin)
                            end.linkTo(parent.end)
                            top.linkTo(textRef.top)
                            bottom.linkTo(parent.bottom)
                            horizontalBias = 0f
                            width = Dimension.value(14.dp)
                            height = Dimension.value(14.dp)
                        },
                        painter = painterResource(id = popupActions.primary.icon),
                        contentDescription = popupActions.primary.name,
                        tint = iconColor,
                    )
                }
            }
        }

        DropdownMenu(
            modifier = Modifier.background(RadixTheme.colors.defaultBackground),
            expanded = isDropdownMenuExpanded,
            onDismissRequest = { isDropdownMenuExpanded = false }
        ) {
            actions?.let { popupActions ->
                popupActions.all.forEach {
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
                            when (val actionData = it.onAction()) {
                                is OnAction.CallbackBasedAction -> actionData.onAction(context)
                                is OnAction.ViewBasedAction -> viewBasedAction = actionData
                            }
                        },
                        contentPadding = PaddingValues(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingXSmall
                        )
                    )
                }
            }
        }

        viewBasedAction?.let { actionData ->
            BottomSheetWrapper(
                onDismissRequest = { viewBasedAction = null },
                bottomSheetState = sheetState
            ) {
                actionData.ActionView()
            }
        }
    }
}

@Composable
private fun resolveAddress(
    address: String,
    truncateAddress: Boolean
): ActionableAddress = remember(address) { ActionableAddress(address, truncateAddress) }

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
private sealed interface OnAction {

    sealed interface ViewBasedAction : OnAction {

        /**
         * This view will appear when the user has clicked on the corresponding [PopupActionItem]
         */
        @Composable
        fun ActionView()

        data class QRCode(private val actionableAddress: ActionableAddress) : ViewBasedAction {

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

    sealed interface CallbackBasedAction : OnAction {

        fun onAction(context: Context)

        data class CopyToClipboard(
            private val actionableAddress: ActionableAddress
        ) : CallbackBasedAction {

            override fun onAction(context: Context) {
                context.getSystemService<android.content.ClipboardManager>()?.let { clipboardManager ->

                    val clipData = ClipData.newPlainText(
                        "Radix Address",
                        actionableAddress.address
                    )

                    clipboardManager.setPrimaryClip(clipData)

                    // From Android 13, the system handles the copy confirmation
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                        Toast.makeText(context, R.string.addressAction_copiedToClipboard, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        data class OpenExternalWebView(
            private val url: String
        ) : CallbackBasedAction {

            @Suppress("SwallowedException")
            override fun onAction(context: Context) {
                context.openUrl(url)
            }
        }

        data class VerifyAddressOnLedger(
            private val actionableAddress: ActionableAddress,
            private val verifyAddressOnLedgerUseCase: VerifyAddressOnLedgerUseCase,
            private val applicationScope: CoroutineScope
        ) : CallbackBasedAction {
            override fun onAction(context: Context) {
                applicationScope.launch {
                    val result = verifyAddressOnLedgerUseCase(actionableAddress.address)
                    withContext(Dispatchers.Main) {
                        result.onSuccess {
                            Toast.makeText(
                                context,
                                R.string.addressAction_verifyAddressLedger_success,
                                Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure {
                            Timber.w(it)
                            Toast.makeText(
                                context,
                                R.string.addressAction_verifyAddressLedger_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface ActionableAddressViewEntryPoint {
    fun profileUseCase(): GetProfileUseCase

    fun verifyAddressOnLedgerUseCase(): VerifyAddressOnLedgerUseCase

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
