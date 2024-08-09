@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.dialogs.address

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dialogs.address.AddressDetailsDialogViewModel.State.Section
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BottomSheetDialogWrapper
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.openUrl
import com.babylon.wallet.android.utils.shareText
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ComponentAddress
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import rdx.works.core.qr.QRCodeGenerator

@Composable
fun AddressDetailsDialog(
    viewModel: AddressDetailsDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var enlargedAddress: EnlargedAddressState? by remember {
        mutableStateOf(null)
    }

    HandleEvents(
        events = viewModel.oneOffEvent,
        onShowEnlargedAddress = { address, numberRanges ->
            enlargedAddress = EnlargedAddressState(
                address = address,
                numberRanges = numberRanges,
                isVisible = true
            )
        },
        onHideEnlargedAddress = {
            enlargedAddress = enlargedAddress?.copy(isVisible = false)
        }
    )

    AddressDetailsDialogContent(
        state = state,
        onCopy = viewModel::onCopyClick,
        onEnlarge = viewModel::onEnlargeClick,
        onShare = viewModel::onShareClick,
        onVisitDashboard = viewModel::onVisitDashboardClick,
        onVerifyAddressOnLedger = viewModel::onVerifyOnLedgerDeviceClick,
        onDismiss = onDismiss
    )

    enlargedAddress?.let {
        EnlargedAddressView(
            enlargedAddressState = it,
            onDismiss = viewModel::onHideEnlargeClick
        )
    }
}

@Composable
private fun AddressDetailsDialogContent(
    modifier: Modifier = Modifier,
    state: AddressDetailsDialogViewModel.State,
    onCopy: () -> Unit,
    onEnlarge: () -> Unit,
    onShare: () -> Unit,
    onVisitDashboard: () -> Unit,
    onVerifyAddressOnLedger: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    BottomSheetDialogWrapper(
        modifier = modifier,
        onDismiss = onDismiss,
        showDragHandle = true,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (state.title != null) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
                        text = state.title,
                        textAlign = TextAlign.Center,
                        style = RadixTheme.typography.title,
                        color = RadixTheme.colors.gray1
                    )
                }

                state.sections.forEach { section ->
                    when (section) {
                        is Section.AccountAddressQRCode -> AccountAddressQRCode(accountAddress = section.accountAddress)
                        is Section.FullAddress -> FullAddress(
                            fullAddress = section.rawAddress,
                            boldRanges = section.boldRanges,
                            onCopy = onCopy,
                            onEnlarge = onEnlarge,
                            onShare = onShare
                        )

                        is Section.VisitDashboard -> VisitDashboard(onClick = onVisitDashboard)
                        is Section.VerifyAddressOnLedger -> VerifyAddressOnLedger(
                            isVerifying = section.isVerifying,
                            onClick = onVerifyAddressOnLedger
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingDefault))
            }
        }
    )
}

@Composable
private fun AccountAddressQRCode(
    modifier: Modifier = Modifier,
    accountAddress: AccountAddress
) {
    val qrCodeBitmap = remember(accountAddress) {
        QRCodeGenerator.forAccount(address = accountAddress)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RadixTheme.dimensions.paddingSmall),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXLarge),
            text = stringResource(id = R.string.addressDetails_qrCode),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXXXXLarge)
                .aspectRatio(ratio = 1f),
            model = qrCodeBitmap,
            contentDescription = null
        )
    }
}

@Composable
private fun FullAddress(
    modifier: Modifier = Modifier,
    fullAddress: String,
    boldRanges: ImmutableList<OpenEndRange<Int>>,
    onCopy: () -> Unit,
    onEnlarge: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingDefault)
            .background(
                color = RadixTheme.colors.gray5,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingLarge
            ),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.addressDetails_fullAddress),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = buildAnnotatedString {
                append(fullAddress)

                boldRanges.forEach { range ->
                    addStyle(
                        style = SpanStyle(color = RadixTheme.colors.gray1),
                        start = range.start,
                        end = range.endExclusive
                    )
                }
            },
            style = RadixTheme.typography.body1Header,
            color = RadixTheme.colors.gray2,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCopy
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.6f),
                    painter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(
                    text = stringResource(id = R.string.addressDetails_copy),
                    style = RadixTheme.typography.body1Header
                )
            }

            TextButton(
                onClick = onEnlarge
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.6f),
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_enlarge),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(
                    text = stringResource(id = R.string.addressDetails_enlarge),
                    style = RadixTheme.typography.body1Header
                )
            }

            TextButton(
                onClick = onShare
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.6f),
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_share),
                    contentDescription = null,
                    tint = RadixTheme.colors.gray1
                )
                Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingXSmall))
                Text(
                    text = stringResource(id = R.string.addressDetails_share),
                    style = RadixTheme.typography.body1Header
                )
            }
        }
    }
}

@Composable
private fun VisitDashboard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RadixSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.addressDetails_viewOnDashboard),
        trailingContent = {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(id = R.drawable.ic_external_link),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        },
        onClick = onClick
    )
}

@Composable
private fun VerifyAddressOnLedger(
    modifier: Modifier = Modifier,
    isVerifying: Boolean,
    onClick: () -> Unit
) {
    RadixSecondaryButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = RadixTheme.dimensions.paddingDefault)
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        text = stringResource(id = R.string.addressDetails_verifyOnLedger),
        onClick = onClick,
        isLoading = isVerifying,
        enabled = !isVerifying
    )
}

@Composable
private fun HandleEvents(
    events: Flow<AddressDetailsDialogViewModel.Event>,
    onShowEnlargedAddress: (
        value: String,
        numberRanges: List<OpenEndRange<Int>>
    ) -> Unit,
    onHideEnlargedAddress: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        events.collect { event ->
            when (event) {
                is AddressDetailsDialogViewModel.Event.CloseEnlarged -> onHideEnlargedAddress()
                is AddressDetailsDialogViewModel.Event.PerformEnlarge -> onShowEnlargedAddress(event.value, event.numberRanges)
                is AddressDetailsDialogViewModel.Event.PerformCopy -> {
                    context.getSystemService<ClipboardManager>()?.let { clipboardManager ->
                        val clipData = ClipData.newPlainText(
                            "Radix Address",
                            event.valueToCopy
                        )

                        clipboardManager.setPrimaryClip(clipData)

                        // From Android 13, the system handles the copy confirmation
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                            Toast.makeText(context, R.string.addressAction_copiedToClipboard, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                is AddressDetailsDialogViewModel.Event.PerformShare -> context.shareText(
                    title = event.shareTitle,
                    value = event.shareValue
                )

                is AddressDetailsDialogViewModel.Event.PerformVisitDashBoard -> context.openUrl(event.url)
                is AddressDetailsDialogViewModel.Event.ShowLedgerVerificationResult -> if (event.isVerified) {
                    Toast.makeText(
                        context,
                        R.string.addressAction_verifyAddressLedger_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
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

private data class EnlargedAddressState(
    val address: String,
    val numberRanges: List<OpenEndRange<Int>>,
    val isVisible: Boolean
)

@Composable
private fun EnlargedAddressView(
    modifier: Modifier = Modifier,
    enlargedAddressState: EnlargedAddressState,
    onDismiss: () -> Unit
) {
    if (enlargedAddressState.isVisible) {
        BasicAlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingXXXLarge,
                        vertical = RadixTheme.dimensions.paddingDefault
                    )
                    .clickable { onDismiss() }
                    .background(color = RadixTheme.colors.gray1.copy(alpha = 0.74f), shape = RadixTheme.shapes.roundedRectMedium)
                    .padding(RadixTheme.dimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                text = buildAnnotatedString {
                    append(enlargedAddressState.address)

                    val greenSpan = SpanStyle(color = RadixTheme.colors.green3)
                    enlargedAddressState.numberRanges.forEach { range ->
                        addStyle(style = greenSpan, start = range.start, end = range.endExclusive)
                    }
                },
                textAlign = TextAlign.Center,
                fontSize = 50.sp,
                lineHeight = 64.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = RadixTheme.colors.white
            )
        }
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentAccountAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            AccountAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Account(v1 = address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "My Main Account",
                sections = listOf(
                    Section.AccountAddressQRCode(accountAddress = address),
                    Section.FullAddress.from(actionableAddress = actionableAddress),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                    Section.VerifyAddressOnLedger(accountAddress = address)
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentResourceAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            ResourceAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Resource(v1 = address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "Radix (XRD)",
                sections = listOf(
                    Section.FullAddress.from(actionableAddress = actionableAddress),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentGlobalIdPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            NonFungibleGlobalId.sample()
        }
        val actionableAddress = ActionableAddress.GlobalId(
            address = address,
            isVisitableInDashboard = true,
            isOnlyLocalIdVisible = false
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = "NFT Collection",
                sections = listOf(
                    Section.FullAddress.from(actionableAddress = actionableAddress),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}

@UsesSampleValues
@Composable
@Preview
fun AddressDetailsDialogContentRandomComponentAddressPreview() {
    RadixWalletPreviewTheme {
        val address = remember {
            ComponentAddress.sampleMainnet()
        }
        val actionableAddress = ActionableAddress.Address(
            address = Address.Component(address),
            isVisitableInDashboard = true
        )
        AddressDetailsDialogContent(
            state = AddressDetailsDialogViewModel.State(
                actionableAddress = actionableAddress,
                title = null,
                sections = listOf(
                    Section.FullAddress.from(actionableAddress = actionableAddress),
                    Section.VisitDashboard(url = actionableAddress.dashboardUrl().orEmpty()),
                )
            ),
            onCopy = {},
            onEnlarge = {},
            onShare = {},
            onVisitDashboard = {},
            onVerifyAddressOnLedger = {},
            onDismiss = { }
        )
    }
}
