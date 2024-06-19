@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.ui.composables.actionableaddress

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.encodeUtf8
import com.babylon.wallet.android.utils.openUrl
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.IntentHash
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.formatted
import com.radixdlt.sargon.extensions.networkId
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import rdx.works.core.sargon.dashboardUrl
import rdx.works.core.sargon.serializers.AddressSerializer
import rdx.works.core.sargon.serializers.IntentHashSerializer
import rdx.works.core.sargon.serializers.NonFungibleGlobalIdSerializer

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: Address,
    isVisitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val actionableAddress by ActionableAddress.remember(
        address = address,
        isVisitableInDashboard = isVisitableInDashboard
    )

    ActionableAddressView(
        modifier = modifier,
        address = actionableAddress,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    globalId: NonFungibleGlobalId,
    isVisitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val actionableAddress by ActionableAddress.remember(
        globalId = globalId,
        isVisitableInDashboard = isVisitableInDashboard
    )

    ActionableAddressView(
        modifier = modifier,
        address = actionableAddress,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

@Composable
fun ActionableAddressView(
    modifier: Modifier = Modifier,
    transactionId: IntentHash,
    isVisitableInDashboard: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    textColor: Color = Color.Unspecified,
    iconColor: Color = textColor
) {
    val actionableAddress by ActionableAddress.remember(
        intentHash = transactionId,
        isVisitableInDashboard = isVisitableInDashboard
    )

    ActionableAddressView(
        modifier = modifier,
        address = actionableAddress,
        textStyle = textStyle,
        textColor = textColor,
        iconColor = iconColor
    )
}

private const val INLINE_ICON_ID = "icon"

@Composable
private fun ActionableAddressView(
    modifier: Modifier = Modifier,
    address: ActionableAddress?,
    textStyle: TextStyle,
    textColor: Color,
    iconColor: Color,
) {
    if (address != null) {
        Box(modifier = modifier) {
            val dependenciesProvider = LocalActionableAddressViewEntryPoint.current
            val coroutineScope = rememberCoroutineScope()

            Text(
                modifier = Modifier.throttleClickable {
                    when (val action = address.action) {
                        is ActionableAddress.Action.DropDown -> action.isExpanded.value = true
                        is ActionableAddress.Action.Modal -> coroutineScope.launch {
                            dependenciesProvider.appEventBus().sendEvent(AppEvent.AddressDetails(address))
                        }
                    }
                },
                text = buildAnnotatedString {
                    append(address.truncated)
                    append(" ")
                    appendInlineContent(id = INLINE_ICON_ID)
                },
                color = textColor,
                maxLines = 1,
                style = textStyle,
                overflow = TextOverflow.Clip,
                inlineContent = mapOf(
                    INLINE_ICON_ID to InlineTextContent(Placeholder(14.sp, 14.sp, PlaceholderVerticalAlign.Center)) {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            painter = address.icon(),
                            contentDescription = address.contentDescription(),
                            tint = iconColor,
                        )
                    }
                )
            )

            (address.action as? ActionableAddress.Action.DropDown)?.let { dropDownAction ->
                DropDown(dropDownAction, address)
            }
        }
    }
}

@Composable
private fun DropDown(
    action: ActionableAddress.Action.DropDown,
    address: ActionableAddress
) {
    val context = LocalContext.current
    DropdownMenu(
        modifier = Modifier.background(RadixTheme.colors.defaultBackground),
        expanded = action.isExpanded.value,
        onDismissRequest = { action.isExpanded.value = false }
    ) {
        action.actions.forEach { actionItem ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = actionItem.name(forAddress = address),
                        style = RadixTheme.typography.body1Regular,
                        color = RadixTheme.colors.defaultText
                    )
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = actionItem.icon(),
                        contentDescription = actionItem.name(forAddress = address),
                        tint = RadixTheme.colors.defaultText
                    )
                },
                onClick = {
                    action.isExpanded.value = false
                    actionItem.performAction(context = context)
                },
                contentPadding = PaddingValues(
                    horizontal = RadixTheme.dimensions.paddingDefault,
                    vertical = RadixTheme.dimensions.paddingXSmall
                )
            )
        }
    }
}

@Serializable
sealed interface ActionableAddress {

    val truncated: String
    val icon: AccompaniedIcon
    val action: Action
    val isVisitableInDashboard: Boolean

    fun copyableAddress(): String

    fun fullAddress(): String

    fun dashboardUrl(): String?

    @Composable
    fun icon(): Painter

    @Composable
    fun contentDescription(): String

    @Serializable
    @SerialName("address")
    data class Address(
        @Serializable(with = AddressSerializer::class)
        val address: com.radixdlt.sargon.Address,
        @SerialName("is_visitable_in_dashboard")
        override val isVisitableInDashboard: Boolean
    ) : ActionableAddress {

        @Transient
        override val truncated: String = address.formatted(AddressFormat.DEFAULT)

        @Transient
        override val icon: AccompaniedIcon = AccompaniedIcon.CopyIcon

        @Transient
        override val action: Action = Action.Modal

        override fun copyableAddress(): String = address.formatted(format = AddressFormat.RAW)

        override fun fullAddress(): String = address.formatted(format = AddressFormat.FULL)

        override fun dashboardUrl(): String? = when (address) {
            is com.radixdlt.sargon.Address.AccessController -> null
            is com.radixdlt.sargon.Address.Account -> "account"
            is com.radixdlt.sargon.Address.Component -> "component"
            is com.radixdlt.sargon.Address.Identity -> null
            is com.radixdlt.sargon.Address.Package -> "package"
            is com.radixdlt.sargon.Address.Pool -> "pool"
            is com.radixdlt.sargon.Address.Resource -> "resource"
            is com.radixdlt.sargon.Address.Validator -> "component"
            is com.radixdlt.sargon.Address.Vault -> null
        }?.let { prefix ->
            "${address.networkId.dashboardUrl()}/$prefix/${address.string.encodeUtf8()}"
        }

        @Composable
        override fun icon() = when (icon) {
            AccompaniedIcon.CopyIcon -> painterResource(id = R.drawable.ic_copy)
        }

        @Composable
        override fun contentDescription() = when (icon) {
            AccompaniedIcon.CopyIcon -> stringResource(id = R.string.addressAction_copyAddress)
        }
    }

    @Serializable
    @SerialName("global_id")
    data class GlobalId(
        @Serializable(with = NonFungibleGlobalIdSerializer::class)
        val address: NonFungibleGlobalId,
        @SerialName("is_visitable_in_dashboard")
        override val isVisitableInDashboard: Boolean
    ) : ActionableAddress {

        @Transient
        override val truncated: String = address.nonFungibleLocalId.formatted(AddressFormat.DEFAULT)

        @Transient
        override val icon: AccompaniedIcon = AccompaniedIcon.CopyIcon

        @Transient
        override val action: Action = Action.Modal

        override fun copyableAddress(): String = address.formatted(format = AddressFormat.RAW)

        override fun dashboardUrl(): String = "${address.resourceAddress.networkId.dashboardUrl()}/nft/${address.string.encodeUtf8()}"

        override fun fullAddress(): String = address.formatted(format = AddressFormat.FULL)

        @Composable
        override fun icon() = when (icon) {
            AccompaniedIcon.CopyIcon -> painterResource(id = R.drawable.ic_copy)
        }

        @Composable
        override fun contentDescription() = when (icon) {
            AccompaniedIcon.CopyIcon -> stringResource(id = R.string.addressAction_copyNftId)
        }
    }

    @Serializable
    @SerialName("transaction_id")
    data class TransactionId(
        @Serializable(with = IntentHashSerializer::class)
        val hash: IntentHash,
        @SerialName("is_visitable_in_dashboard")
        override val isVisitableInDashboard: Boolean
    ) : ActionableAddress {

        @Transient
        override val truncated: String = hash.formatted(AddressFormat.DEFAULT)

        @Transient
        override val icon: AccompaniedIcon = AccompaniedIcon.CopyIcon

        @Transient
        override val action: Action = Action.DropDown(
            isExpanded = mutableStateOf(false),
            actions = mutableSetOf<Action.DropDown.ActionItem>(
                Action.DropDown.ActionItem.Copy(value = copyableAddress())
            ).apply {
                if (isVisitableInDashboard) {
                    add(Action.DropDown.ActionItem.Dashboard(url = dashboardUrl()))
                }
            }
        )

        override fun copyableAddress(): String = hash.formatted(format = AddressFormat.RAW)

        override fun dashboardUrl(): String = "${hash.networkId.dashboardUrl()}/transaction/${hash.bech32EncodedTxId.encodeUtf8()}"

        override fun fullAddress(): String = hash.formatted(format = AddressFormat.FULL)

        @Composable
        override fun icon() = when (icon) {
            AccompaniedIcon.CopyIcon -> painterResource(id = R.drawable.ic_copy)
        }

        @Composable
        override fun contentDescription() = when (icon) {
            AccompaniedIcon.CopyIcon -> stringResource(id = R.string.addressAction_copyTransactionId)
        }
    }

    enum class AccompaniedIcon {
        CopyIcon
    }

    sealed interface Action {

        data class DropDown(
            val isExpanded: MutableState<Boolean>,
            val actions: Set<ActionItem>
        ) : Action {

            sealed interface ActionItem {
                data class Copy(val value: String) : ActionItem
                data class Dashboard(val url: String) : ActionItem

                @Composable
                fun name(forAddress: ActionableAddress): String = when (this) {
                    is Copy -> when (forAddress) {
                        is GlobalId -> stringResource(id = R.string.addressAction_copyNftId)
                        is TransactionId -> stringResource(id = R.string.addressAction_copyTransactionId)
                        else -> stringResource(id = R.string.addressAction_copyAddress)
                    }

                    is Dashboard -> stringResource(id = R.string.addressAction_viewOnDashboard)
                }

                @Composable
                fun icon(): Painter = when (this) {
                    is Copy -> painterResource(id = R.drawable.ic_copy)
                    is Dashboard -> painterResource(id = R.drawable.ic_external_link)
                }

                fun performAction(context: Context) {
                    when (this) {
                        is Copy -> {
                            context.getSystemService<android.content.ClipboardManager>()?.let { clipboardManager ->

                                val clipData = ClipData.newPlainText(
                                    "Radix Address",
                                    value
                                )

                                clipboardManager.setPrimaryClip(clipData)

                                // From Android 13, the system handles the copy confirmation
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                    Toast.makeText(context, R.string.addressAction_copiedToClipboard, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        is Dashboard -> context.openUrl(url)
                    }
                }
            }
        }

        data object Modal : Action
    }

    companion object {

        @Composable
        fun remember(
            address: com.radixdlt.sargon.Address,
            isVisitableInDashboard: Boolean = true,
        ): State<ActionableAddress?> {
            val actionableAddress = remember {
                mutableStateOf<Address?>(null)
            }

            LaunchedEffect(key1 = address) {
                actionableAddress.value = Address(address, isVisitableInDashboard)
            }

            return actionableAddress
        }

        @Composable
        fun remember(
            globalId: NonFungibleGlobalId,
            isVisitableInDashboard: Boolean = true
        ): State<ActionableAddress?> {
            val actionableAddress = remember {
                mutableStateOf<GlobalId?>(null)
            }

            LaunchedEffect(key1 = globalId) {
                actionableAddress.value = GlobalId(globalId, isVisitableInDashboard)
            }

            return actionableAddress
        }

        @Composable
        fun remember(
            intentHash: IntentHash,
            isVisitableInDashboard: Boolean = true
        ): State<ActionableAddress?> {
            val actionableAddress = remember {
                mutableStateOf<TransactionId?>(null)
            }

            LaunchedEffect(key1 = intentHash) {
                actionableAddress.value = TransactionId(intentHash, isVisitableInDashboard)
            }

            return actionableAddress
        }

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

@UsesSampleValues
@Preview(showBackground = true)
@Composable
fun ActionableAddressViewTransactionIdPreview() {
    RadixWalletPreviewTheme {
        ActionableAddressView(transactionId = IntentHash.sample())
    }
}
