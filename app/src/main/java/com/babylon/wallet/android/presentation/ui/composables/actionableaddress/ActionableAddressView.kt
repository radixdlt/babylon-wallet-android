@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.ui.composables.actionableaddress

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.usecases.VerifyAddressOnLedgerUseCase
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.AccountQRCodeView
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetWrapper
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.discriminant
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.gateways
import timber.log.Timber

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: Address,
    truncateAddress: Boolean = true,
    visitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    ActionableAddressView(
        modifier = modifier,
        address = ActionableAddress.Address(address),
        truncateAddress = truncateAddress,
        visitableInDashboard = visitableInDashboard,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    globalId: NonFungibleGlobalId,
    truncateAddress: Boolean = true,
    visitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    ActionableAddressView(
        modifier = modifier,
        address = ActionableAddress.GlobalId(globalId),
        truncateAddress = truncateAddress,
        visitableInDashboard = visitableInDashboard,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    localId: NonFungibleLocalId,
    truncateAddress: Boolean = true,
    visitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    ActionableAddressView(
        modifier = modifier,
        address = ActionableAddress.LocalId(localId),
        truncateAddress = truncateAddress,
        visitableInDashboard = visitableInDashboard,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    transactionId: SignedIntentHash,
    truncateAddress: Boolean = true,
    visitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    ActionableAddressView(
        modifier = modifier,
        address = ActionableAddress.TransactionId(transactionId),
        truncateAddress = truncateAddress,
        visitableInDashboard = visitableInDashboard,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: ActionableAddress,
    truncateAddress: Boolean,
    visitableInDashboard: Boolean,
    textStyle: TextStyle,
    textColor: Color,
    iconColor: Color
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useCaseProvider = LocalActionableAddressViewEntryPoint.current

    var actions: PopupActions? by remember(address) {
        mutableStateOf(null)
    }

    LaunchedEffect(address) {
        scope.launch {
            val networkId = if (useCaseProvider.profileUseCase().isInitialized()) {
                useCaseProvider.profileUseCase().gateways.first().current().network.id
            } else {
                Radix.Network.mainnet.id
            }.let { NetworkId.init(discriminant = it.toUByte()) }

            val copyAction = PopupActionItem(
                name = context.getString(
                    when (address) {
                        is ActionableAddress.TransactionId -> R.string.addressAction_copyTransactionId
                        is ActionableAddress.LocalId, is ActionableAddress.GlobalId -> R.string.addressAction_copyNftId
                        else -> R.string.addressAction_copyAddress
                    }
                ),
                icon = R.drawable.ic_copy
            ) { OnAction.CallbackBasedAction.CopyToClipboard(address) }

            val openExternalAction = address
                .takeIf { visitableInDashboard }
                ?.dashboardUrl(networkId)
                ?.let { url ->
                    PopupActionItem(
                        name = context.getString(R.string.addressAction_viewOnDashboard),
                        icon = R.drawable.ic_external_link,
                    ) {
                        OnAction.CallbackBasedAction.OpenExternalWebView(url.toUri())
                    }
                }

            val qrAction = address.asAccountAddress?.let {
                PopupActionItem(
                    name = context.getString(R.string.addressAction_showAccountQR),
                    icon = com.babylon.wallet.android.designsystem.R.drawable.ic_qr_code_scanner
                ) { OnAction.ViewBasedAction.QRCode(it) }
            }

            val primary = if (address.isTransactionId && openExternalAction != null) {
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
            address.asAccountAddress?.let { accountAddress ->
                if (useCaseProvider.profileUseCase().accountOnCurrentNetwork(accountAddress)?.isLedgerAccount == true) {
                    val verifyOnLedgerAction = PopupActionItem(
                        name = context.getString(R.string.addressAction_verifyAddressLedger),
                        icon = com.babylon.wallet.android.designsystem.R.drawable.ic_ledger_hardware_wallets
                    ) {
                        OnAction.CallbackBasedAction.VerifyAddressOnLedger(
                            accountAddress = accountAddress,
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
                append(address.formatted(format = if (truncateAddress) AddressFormat.DEFAULT else AddressFormat.FULL))
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

        data class QRCode(private val accountAddress: AccountAddress) : ViewBasedAction {

            @Composable
            override fun ActionView() {
                Box(
                    modifier = Modifier
                        .background(RadixTheme.colors.defaultBackground)
                ) {
                    AccountQRCodeView(accountAddress = accountAddress)
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
                        actionableAddress.formatted(format = AddressFormat.RAW)
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
            private val url: Uri
        ) : CallbackBasedAction {

            @Suppress("SwallowedException")
            override fun onAction(context: Context) {
                context.openUrl(url)
            }
        }

        data class VerifyAddressOnLedger(
            private val accountAddress: AccountAddress,
            private val verifyAddressOnLedgerUseCase: VerifyAddressOnLedgerUseCase,
            private val applicationScope: CoroutineScope
        ) : CallbackBasedAction {
            override fun onAction(context: Context) {
                applicationScope.launch {
                    val result = verifyAddressOnLedgerUseCase(accountAddress)
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

@VisibleForTesting
sealed interface ActionableAddress {

    fun formatted(format: AddressFormat = AddressFormat.DEFAULT): String

    fun dashboardSuffix(): String?

    fun dashboardUrl(networkId: NetworkId): String? = dashboardSuffix()?.let { suffix ->
        val dashboard = rdx.works.profile.derivation.model.NetworkId.from(networkId.discriminant.toInt()).dashboardUrl()

        "$dashboard/$suffix"
    }

    val asAccountAddress: AccountAddress?
        get() = (this as? Address)?.let { it.address as? com.radixdlt.sargon.Address.Account }?.v1

    val isTransactionId: Boolean
        get() = this is TransactionId

    data class Address(
        val address: com.radixdlt.sargon.Address
    ) : ActionableAddress {
        override fun formatted(format: AddressFormat): String = address.formatted(format = format)

        override fun dashboardSuffix(): String? = when (address) {
            is com.radixdlt.sargon.Address.AccessController -> null
            is com.radixdlt.sargon.Address.Account -> "account"
            is com.radixdlt.sargon.Address.Component -> "component"
            is com.radixdlt.sargon.Address.Identity -> null
            is com.radixdlt.sargon.Address.Package -> "package"
            is com.radixdlt.sargon.Address.Pool -> "pool"
            is com.radixdlt.sargon.Address.Resource -> "resource"
            is com.radixdlt.sargon.Address.Validator -> "component"
            is com.radixdlt.sargon.Address.Vault -> null
        }?.let {
            "$it/${address.string.encodeUtf8()}"
        }
    }

    data class GlobalId(
        val address: NonFungibleGlobalId
    ) : ActionableAddress {
        override fun formatted(format: AddressFormat): String = address.formatted(format = format)

        override fun dashboardSuffix(): String = "nft/${address.string.encodeUtf8()}"
    }

    data class LocalId(
        val address: NonFungibleLocalId
    ) : ActionableAddress {
        override fun formatted(format: AddressFormat): String = address.formatted(format = format)
        override fun dashboardSuffix(): String? = null
    }

    data class TransactionId(
        val hash: SignedIntentHash
    ) : ActionableAddress {
        override fun formatted(format: AddressFormat): String = hash.formatted(format = format)
        override fun dashboardSuffix(): String = "transaction/${hash.bech32EncodedTxId.encodeUtf8()}"
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ActionableAddressViewPreview() {
    RadixWalletPreviewTheme {
        ActionableAddressView(address = Address.Account(AccountAddress.sampleMainnet()))
    }
}
